package com.pos.posApps.Service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.ProductEntity;
import com.pos.posApps.Entity.PurchasingEntity;
import com.pos.posApps.Entity.TransactionEntity;
import com.pos.posApps.Repository.ProductRepository;
import com.pos.posApps.Repository.PurchasingRepository;
import com.pos.posApps.Repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.*;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LaporanService {
    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    PurchasingRepository purchasingRepository;

//    public List<LaporanNilaiPersediaanDTO> getLaporanNilaiPersediaan(long clientId) {
//        List<ProductEntity> productData = productRepository.findAllByClientEntity_ClientIdAndProductPricesEntityIsNotNullAndDeletedAtIsNullOrderByProductIdDesc(clientId);
//        return productData.stream().map(product -> new LaporanNilaiPersediaanDTO(
//                product.getShortName(),
//                product.getFullName(),
//                product.getStock(),
//                product.getSupplierPrice(),
//                product.getProductPricesEntity().get(0).getPrice(),
//                BigDecimal.valueOf(product.getStock()).multiply(product.getSupplierPrice())
//        )).collect(Collectors.toList());
//    }

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
        List<TransactionEntity> transactionData;

        if (customerId == null) {
            transactionData = transactionRepository
                    .findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(
                            clientId, startDate, endDate);
        } else {
            transactionData = transactionRepository
                    .findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(
                            clientId, customerId, startDate, endDate);
        }

        DateTimeFormatter formatter = switch (filterOptions != null ? filterOptions.toLowerCase() : "") {
            case "year" -> DateTimeFormatter.ofPattern("yyyy");
            case "month" -> DateTimeFormatter.ofPattern("yyyy-MM");
            default -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
        };

        // Generate all periods between start and end
        List<String> allPeriods = generateAllPeriods(startDate.toLocalDate(), endDate.toLocalDate(), filterOptions);

        // Preload semua produk (asumsikan shortName unik)
        Map<String, ProductEntity> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(ProductEntity::getShortName, Function.identity(), (a, b) -> a));

        // Grouping by actual data
        Map<String, LaporanPenjualanPerWaktuDTO> groupedMap = transactionData.stream()
                .flatMap(trx -> trx.getTransactionDetailEntities().stream()
                        .filter(detail -> detail.getDeletedAt() == null)
                        .map(detail -> {
                            String period = trx.getCreatedAt().format(formatter);
                            BigDecimal hargaJual = detail.getPrice();
                            BigDecimal totalPrice = detail.getTotalPrice();
                            Long qty = detail.getQty();

                            ProductEntity product = productMap.get(detail.getShortName());
                            BigDecimal hargaBeli = (product != null) ? product.getSupplierPrice() : BigDecimal.ZERO;
                            BigDecimal laba = hargaJual.subtract(hargaBeli).multiply(BigDecimal.valueOf(qty));

                            return Map.entry(period, new LaporanPenjualanPerWaktuDTO(period, totalPrice, laba));
                        })
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, incoming) -> new LaporanPenjualanPerWaktuDTO(
                                existing.getPeriod(),
                                existing.getTotalHargaPenjualan().add(incoming.getTotalHargaPenjualan()),
                                existing.getLabaPenjualan().add(incoming.getLabaPenjualan())
                        )
                ));

        // Ensure all periods are present in the final map, even if zero
        Map<String, LaporanPenjualanPerWaktuDTO> finalMap = new TreeMap<>(); // TreeMap to ensure natural ordering
        for (String period : allPeriods) {
            LaporanPenjualanPerWaktuDTO data = groupedMap.getOrDefault(
                    period,
                    new LaporanPenjualanPerWaktuDTO(period, BigDecimal.ZERO, BigDecimal.ZERO)
            );
            finalMap.put(period, data);
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
        List<PurchasingEntity> purchasingData;
        if (supplierId == null) {
            purchasingData = purchasingRepository.findAllByClientEntity_ClientIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(clientId, startDate, endDate);
        } else {
            purchasingData = purchasingRepository.findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(clientId, supplierId, startDate, endDate);
        }

        DateTimeFormatter formatter = switch (filterOptions != null ? filterOptions.toLowerCase() : "") {
            case "year" -> DateTimeFormatter.ofPattern("yyyy");
            case "month" -> DateTimeFormatter.ofPattern("yyyy-MM");
            default -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
        };

        // Generate all periods between start and end
        List<String> allPeriods = generateAllPeriods(startDate.toLocalDate(), endDate.toLocalDate(), filterOptions);

        // Preload semua produk (asumsikan shortName unik)
        Map<String, ProductEntity> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(ProductEntity::getShortName, Function.identity(), (a, b) -> a));

        // Grouping by actual data
        Map<String, LaporanPembelianPerWaktuDTO> groupedMap = purchasingData.stream()
                .flatMap(trx -> trx.getPurchasingDetailEntities().stream()
                                .filter(detail -> detail.getDeletedAt() == null)
                                .map(detail -> {
                                    String period = trx.getCreatedAt().format(formatter);
//                            BigDecimal hargaJual = detail.getPrice();
                                    BigDecimal totalPrice = detail.getTotalPrice();
//                            Long qty = detail.getQty();

                                    ProductEntity product = productMap.get(detail.getShortName());
                                    BigDecimal hargaBeli = (product != null) ? product.getSupplierPrice() : BigDecimal.ZERO;
//                            BigDecimal laba = hargaJual.subtract(hargaBeli).multiply(BigDecimal.valueOf(qty));

                                    return Map.entry(period, new LaporanPembelianPerWaktuDTO(period, totalPrice));
                                })
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, incoming) -> new LaporanPembelianPerWaktuDTO(
                                existing.getPeriod(),
                                existing.getTotalHargaPembelian().add(incoming.getTotalHargaPembelian())
                        )
                ));

        // Ensure all periods are present in the final map, even if zero
        Map<String, LaporanPembelianPerWaktuDTO> finalMap = new TreeMap<>(); // TreeMap to ensure natural ordering
        for (String period : allPeriods) {
            LaporanPembelianPerWaktuDTO data = groupedMap.getOrDefault(
                    period,
                    new LaporanPembelianPerWaktuDTO(period, BigDecimal.ZERO)
            );
            finalMap.put(period, data);
        }

        return new ArrayList<>(finalMap.values());
    }

    public List<LaporanPenjualanPerPelangganDTO> getLaporanPenjualanDataByCustomer(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<TransactionEntity> transactionData;

        transactionData = transactionRepository
                .findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(
                        clientId, startDate, endDate);


        // Preload all products (assuming shortName is unique)
        Map<String, ProductEntity> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(ProductEntity::getShortName, Function.identity(), (a, b) -> a));

        // Group by customer name
        Map<String, LaporanPenjualanPerPelangganDTO> groupedByCustomer = transactionData.stream()
                .flatMap(trx -> trx.getTransactionDetailEntities().stream()
                        .filter(detail -> detail.getDeletedAt() == null)
                        .map(detail -> {
                            String customerName = trx.getCustomerEntity() != null
                                    ? trx.getCustomerEntity().getName()
                                    : "Unknown Customer";

                            BigDecimal hargaJual = detail.getPrice();
                            BigDecimal totalPrice = detail.getTotalPrice();
                            Long qty = detail.getQty();

                            ProductEntity product = productMap.get(detail.getShortName());
                            BigDecimal hargaBeli = (product != null) ? product.getSupplierPrice() : BigDecimal.ZERO;
                            BigDecimal laba = hargaJual.subtract(hargaBeli).multiply(BigDecimal.valueOf(qty));

                            return Map.entry(customerName,
                                    new LaporanPenjualanPerPelangganDTO(customerName, totalPrice, laba));
                        })
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, incoming) -> new LaporanPenjualanPerPelangganDTO(
                                existing.getCustomerName(),
                                existing.getTotalHargaPenjualan().add(incoming.getTotalHargaPenjualan()),
                                existing.getLabaPenjualan().add(incoming.getLabaPenjualan())
                        )
                ));

        return groupedByCustomer.values().stream()
                .sorted(Comparator.comparing(LaporanPenjualanPerPelangganDTO::getCustomerName))
                .collect(Collectors.toList());
    }

    public List<LaporanPembelianPerPelangganDTO> getLaporanPembelianDataByCustomer(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<PurchasingEntity> purchasingData;

        purchasingData = purchasingRepository.findAllByClientEntity_ClientIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(clientId, startDate, endDate);

//        transactionData = transactionRepository
//                .findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(
//                        clientId, startDate, endDate);


        // Preload all products (assuming shortName is unique)
        Map<String, ProductEntity> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(ProductEntity::getShortName, Function.identity(), (a, b) -> a));

        // Group by customer name
        Map<String, LaporanPembelianPerPelangganDTO> groupedByCustomer = purchasingData.stream()
                .flatMap(trx -> trx.getPurchasingDetailEntities().stream()
                                .filter(detail -> detail.getDeletedAt() == null)
                                .map(detail -> {
                                    String supplierName = trx.getSupplierEntity() != null
                                            ? trx.getSupplierEntity().getSupplierName()
                                            : "Unknown Supplier";

//                            BigDecimal hargaJual = detail.getPrice();
                                    BigDecimal totalPrice = detail.getTotalPrice();
//                            Long qty = detail.getQty();

//                            ProductEntity product = productMap.get(detail.getShortName());
//                            BigDecimal hargaBeli = (product != null) ? product.getSupplierPrice() : BigDecimal.ZERO;

                                    return Map.entry(supplierName,
                                            new LaporanPembelianPerPelangganDTO(supplierName, totalPrice));
                                })
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, incoming) -> new LaporanPembelianPerPelangganDTO(
                                existing.getSupplierName(),
                                existing.getTotalHargaPembelian().add(incoming.getTotalHargaPembelian())
                        )
                ));

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

    @Transactional(readOnly = true)
    public void exportLaporanNilaiPersediaanStream(long clientId, OutputStream outputStream) {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        try (Stream<ProductEntity> productStream = productRepository.streamAllByClientId(clientId)) {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setFullCompression(); // PDF lebih kecil
            document.open();

            // Header
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            document.add(new Paragraph("Laporan Nilai Persediaan Barang", headerFont));
            document.add(new Paragraph("Client ID: " + clientId));
            document.add(Chunk.NEWLINE);

            // Table header
            PdfPTable headerTable = new PdfPTable(6);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{2, 4, 2, 2, 2, 2});

            Stream.of("Kode", "Nama Barang", "Stock", "Harga Beli", "Harga Jual", "Total Nilai")
                    .forEach(title -> {
                        PdfPCell cell = new PdfPCell(new Phrase(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                        cell.setBackgroundColor(Color.LIGHT_GRAY);
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        headerTable.addCell(cell);
                    });
            document.add(headerTable);

            // Buffer table kecil (biar nggak berat)
            PdfPTable chunkTable = new PdfPTable(6);
            chunkTable.setWidthPercentage(100);
            chunkTable.setWidths(new float[]{2, 4, 2, 2, 2, 2});

            final int FLUSH_INTERVAL = 1000; // flush tiap 1000 baris
            final int[] counter = {0};

            productStream.forEach(product -> {
                try {
                    BigDecimal hargaJual = product.getProductPricesEntity().isEmpty()
                            ? BigDecimal.ZERO
                            : product.getProductPricesEntity().get(0).getPrice();

                    BigDecimal totalNilai = BigDecimal
                            .valueOf(product.getStock())
                            .multiply(product.getSupplierPrice());

                    chunkTable.addCell(new Phrase(product.getShortName()));
                    chunkTable.addCell(new Phrase(product.getFullName()));
                    chunkTable.addCell(new Phrase(String.valueOf(product.getStock())));
                    chunkTable.addCell(new Phrase(formatRupiah(product.getSupplierPrice())));
                    chunkTable.addCell(new Phrase(formatRupiah(hargaJual)));
                    chunkTable.addCell(new Phrase(formatRupiah(totalNilai)));

                    counter[0]++;
                    if (counter[0] % FLUSH_INTERVAL == 0) {
                        document.add(chunkTable);
                        chunkTable.flushContent(); // kosongkan isi table tanpa reset struktur
                        writer.flush(); // paksa tulis ke OutputStream
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Error saat tulis PDF baris: " + ex.getMessage(), ex);
                }
            });

            // Tambahkan sisa data terakhir
            if (counter[0] % FLUSH_INTERVAL != 0) {
                document.add(chunkTable);
            }

            document.close();
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Gagal generate PDF streaming", e);
        }
    }

    private String formatRupiah(BigDecimal value) {
        return String.format("Rp%,.0f", value).replace(",", ".");
    }
}
