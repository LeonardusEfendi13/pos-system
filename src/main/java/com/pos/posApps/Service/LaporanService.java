package com.pos.posApps.Service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.ProductEntity;
import com.pos.posApps.Entity.PurchasingEntity;
import com.pos.posApps.Entity.TransactionDetailEntity;
import com.pos.posApps.Entity.TransactionEntity;
import com.pos.posApps.Repository.ProductRepository;
import com.pos.posApps.Repository.PurchasingRepository;
import com.pos.posApps.Repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.*;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.awt.Color.LIGHT_GRAY;

@Service
public class LaporanService {
    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    PurchasingRepository purchasingRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    public String getTotalAsset(long clientId) {
        return formatRupiah(productRepository.sumInventoryValue(clientId));

    }

    public Page<LaporanNilaiPersediaanDTO> getLaporanNilaiPersediaan(long clientId, Pageable pageable) {
        Page<Long> productIdsPage = productRepository.findProductIds(clientId, pageable);
        List<ProductEntity> products = productRepository.findAllById(productIdsPage.getContent());
        List<LaporanNilaiPersediaanDTO> dtos = products.stream()
                .map(product -> new LaporanNilaiPersediaanDTO(
                        product.getShortName(),
                        product.getFullName(),
                        product.getStock(),
                        product.getSupplierPrice(),
                        product.getProductPricesEntity().isEmpty()
                                ? BigDecimal.ZERO
                                : product.getProductPricesEntity().get(0).getPrice(),
                        BigDecimal.valueOf(product.getStock()).multiply(product.getSupplierPrice())
                ))
                .toList();

        return new PageImpl<>(dtos, pageable, productIdsPage.getTotalElements());
    }


    public List<LaporanPenjualanPerWaktuDTO> getLaporanPenjualanDataByPeriode(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long customerId,
            String filterOptions
    ) {

        String filter = filterOptions != null ? filterOptions.toLowerCase() : "day";

        // 1️⃣ Ambil data yang SUDAH teragregasi
        List<Object[]> rawData =
                transactionRepository.getLaporanPenjualanPerWaktuRaw(
                        clientId, startDate, endDate, customerId, filter
                );

        // 2️⃣ Convert ke Map (PERIOD -> DTO)
        Map<String, LaporanPenjualanPerWaktuDTO> dbMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> new LaporanPenjualanPerWaktuDTO(
                                (String) row[0],
                                (BigDecimal) row[1],
                                (BigDecimal) row[2]
                        )
                ));

        // 3️⃣ Generate semua periode (tetap perlu)
        List<String> allPeriods =
                generateAllPeriods(startDate.toLocalDate(), endDate.toLocalDate(), filter);

        // 4️⃣ Gabungkan + isi data kosong
        Map<String, LaporanPenjualanPerWaktuDTO> finalMap =
                new TreeMap<>(Comparator.reverseOrder());

        for (String period : allPeriods) {
            finalMap.put(
                    period,
                    dbMap.getOrDefault(
                            period,
                            new LaporanPenjualanPerWaktuDTO(
                                    period, BigDecimal.ZERO, BigDecimal.ZERO
                            )
                    )
            );
        }

        return new ArrayList<>(finalMap.values());
    }

    public List<LaporanPembelianPerWaktuDTO> getLaporanPengeluaranDataByPeriode(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long supplierId,
            String filterOptions
    ) {
        // 1️⃣ Ambil data dari repository
        List<PurchasingEntity> purchasingData;
        if (supplierId == null) {
            purchasingData = purchasingRepository
                    .findAllByClientEntity_ClientIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndPoDateBetweenOrderByPurchasingIdDesc(
                            clientId, startDate, endDate);
        } else {
            purchasingData = purchasingRepository
                    .findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndPoDateBetweenOrderByPurchasingIdDesc(
                            clientId, supplierId, startDate, endDate);
        }

        // 2️⃣ Tentukan format periode (harian / bulanan / tahunan)
        DateTimeFormatter formatter = switch (filterOptions != null ? filterOptions.toLowerCase() : "") {
            case "year" -> DateTimeFormatter.ofPattern("yyyy");
            case "month" -> DateTimeFormatter.ofPattern("yyyy-MM");
            default -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
        };

        // 3️⃣ Generate semua periode agar hasil tetap lengkap (termasuk yg kosong)
        List<String> allPeriods = generateAllPeriods(startDate.toLocalDate(), endDate.toLocalDate(), filterOptions);

        // 4️⃣ Group berdasarkan periode (pakai totalPrice langsung dari PurchasingEntity)
        Map<String, LaporanPembelianPerWaktuDTO> groupedMap = purchasingData.stream()
                .map(trx -> {
                    BigDecimal totalPembelian = trx.getTotalPrice() != null ? trx.getTotalPrice() : BigDecimal.ZERO;
                    String period = trx.getPoDate().format(formatter);
                    return Map.entry(period, new LaporanPembelianPerWaktuDTO(period, totalPembelian));
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, incoming) -> {
                            BigDecimal newTotal = existing.getTotalHargaPembelian()
                                    .add(incoming.getTotalHargaPembelian());
                            return new LaporanPembelianPerWaktuDTO(existing.getPeriod(), newTotal);
                        }
                ));

        // 5️⃣ Pastikan semua periode ada di hasil akhir (termasuk nol)
        Map<String, LaporanPembelianPerWaktuDTO> finalMap = new TreeMap<>(Collections.reverseOrder());
        for (String period : allPeriods) {
            LaporanPembelianPerWaktuDTO data = groupedMap.getOrDefault(
                    period,
                    new LaporanPembelianPerWaktuDTO(period, BigDecimal.ZERO)
            );
            finalMap.put(period, data);
        }

        // 7️⃣ Return hasil laporan
        return new ArrayList<>(finalMap.values());
    }

    public List<LaporanPenjualanPerPelangganDTO> getLaporanPenjualanDataByCustomer(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<Object[]> rows =
                transactionRepository.getLaporanPenjualanRaw(
                        clientId, startDate, endDate
                );

        List<LaporanPenjualanPerPelangganDTO> result = new ArrayList<>(rows.size());

        for (Object[] row : rows) {
            result.add(new LaporanPenjualanPerPelangganDTO(
                    (String) row[0],
                    (BigDecimal) row[1],
                    (BigDecimal) row[2]
            ));
        }

        return result;
    }

    public List<LaporanPembelianPerPelangganDTO> getLaporanPembelianDataByCustomer(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        // Ambil semua data purchasing milik client dalam rentang tanggal
        List<PurchasingEntity> purchasingData = purchasingRepository
                .findAllByClientEntity_ClientIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndPoDateBetweenOrderByPurchasingIdDesc(
                        clientId, startDate, endDate
                );

        // Group by supplier dan jumlahkan totalPrice
        Map<String, LaporanPembelianPerPelangganDTO> groupedByCustomer = purchasingData.stream()
                .filter(trx -> trx.getSupplierEntity() != null)
                .collect(Collectors.toMap(
                        trx -> trx.getSupplierEntity().getSupplierName(),
                        trx -> new LaporanPembelianPerPelangganDTO(
                                trx.getSupplierEntity().getSupplierName(),
                                trx.getTotalPrice() != null ? trx.getTotalPrice() : BigDecimal.ZERO
                        ),
                        (existing, incoming) -> new LaporanPembelianPerPelangganDTO(
                                existing.getSupplierName(),
                                existing.getTotalHargaPembelian().add(incoming.getTotalHargaPembelian())
                        )
                ));

        // Urutkan hasil berdasarkan nama supplier
        return groupedByCustomer.values().stream()
                .sorted(Comparator.comparing(LaporanPembelianPerPelangganDTO::getSupplierName))
                .collect(Collectors.toList());
    }


    private List<String> generateAllPeriods(LocalDate startDate, LocalDate endDate, String filterOptions) {
        List<String> periods = new ArrayList<>();
        DateTimeFormatter formatter;

        switch (filterOptions != null ? filterOptions.toLowerCase() : "") {
            case "year":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                Year startYear = Year.from(startDate);
                Year endYear = Year.from(endDate);
                for (Year year = startYear; !year.isAfter(endYear); year = year.plusYears(1)) {
                    periods.add(year.toString());
                }
                break;

            case "month":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                YearMonth startMonth = YearMonth.from(startDate);
                YearMonth endMonth = YearMonth.from(endDate);
                for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
                    periods.add(ym.format(formatter));
                }
                break;

            default: // day
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    periods.add(date.format(formatter));
                }
                break;
        }

        return periods;
    }

    public void exportLaporanNilaiPersediaanStream(Long clientId, OutputStream outputStream) {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        String sql = "SELECT p.short_name, p.full_name, p.stock, p.supplier_price, " +
                "COALESCE(pp.price, 0) AS harga_jual " +
                "FROM product p " +
                "LEFT JOIN (" +
                "    SELECT DISTINCT ON (product_id) product_id, price " +
                "    FROM product_prices " +
                "    WHERE deleted_at IS NULL " +
                "    ORDER BY product_id, product_prices_id" +
                ") pp ON pp.product_id = p.product_id " +
                "WHERE p.client_id = ? " +
                "AND p.deleted_at IS NULL " +
                "ORDER BY p.stock DESC";

        try {
            //Get total asset
            BigDecimal totalAsset = productRepository.sumInventoryValue(clientId);

            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setFullCompression();
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph title = new Paragraph("Laporan Nilai Persediaan Barang", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph clientInfo = new Paragraph("Total Nilai Barang : " + formatRupiah(totalAsset));
            clientInfo.setAlignment(Element.ALIGN_CENTER);
            document.add(clientInfo);
            document.add(Chunk.NEWLINE);

            // Table structure
            float[] columnWidths = {3, 5, 1, 2, 2, 2};
            String[] headers = {"Kode", "Nama Barang", "Stock", "Harga Beli", "Harga Jual", "Total Nilai"};
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            // Create header table
            PdfPTable headerTable = new PdfPTable(6);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(columnWidths);

            for (String header : headers) {
                PdfPCell cell = createCell(header, headerFont, Element.ALIGN_CENTER, LIGHT_GRAY);
                headerTable.addCell(cell);
            }

            document.add(headerTable);

            // Data table
            PdfPTable dataTable = new PdfPTable(6);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(columnWidths);

            final int FLUSH_INTERVAL = 200;
            final int[] counter = {0};

            jdbcTemplate.query(sql, new Object[]{clientId}, (ResultSet rs) -> {
                try {
                    String kode = rs.getString("short_name");
                    String nama = rs.getString("full_name");
                    int stock = rs.getInt("stock");
                    BigDecimal hargaBeli = rs.getBigDecimal("supplier_price");
                    BigDecimal hargaJual = rs.getBigDecimal("harga_jual");
                    BigDecimal totalNilai = hargaBeli.multiply(BigDecimal.valueOf(stock));

                    dataTable.addCell(createCell(kode, Element.ALIGN_LEFT));
                    dataTable.addCell(createCell(nama, Element.ALIGN_LEFT));
                    dataTable.addCell(createCell(String.valueOf(stock), Element.ALIGN_CENTER));
                    dataTable.addCell(createCell(formatRupiah(hargaBeli), Element.ALIGN_LEFT));
                    dataTable.addCell(createCell(formatRupiah(hargaJual), Element.ALIGN_LEFT));
                    dataTable.addCell(createCell(formatRupiah(totalNilai), Element.ALIGN_LEFT));

                    counter[0]++;
                    if (counter[0] % FLUSH_INTERVAL == 0) {
                        document.add(dataTable);
                        dataTable.flushContent();
                        writer.flush();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error saat tulis PDF baris: " + e.getMessage(), e);
                }
            });

            // Add remaining rows
            if (counter[0] % FLUSH_INTERVAL != 0) {
                document.add(dataTable);
            }

            document.close();
            writer.flush();

        } catch (Exception e) {
            throw new RuntimeException("Gagal generate PDF streaming", e);
        }
    }

    private PdfPCell createCell(String content, int alignment) {
        return createCell(content, FontFactory.getFont(FontFactory.HELVETICA, 10), alignment, null);
    }

    private PdfPCell createCell(String content, Font font, int alignment, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        return cell;
    }

    private String formatRupiah(BigDecimal value) {
        return String.format("Rp %,.0f", value).replace(",", ".");
    }
}
