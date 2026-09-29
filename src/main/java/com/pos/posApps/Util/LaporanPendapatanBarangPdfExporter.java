package com.pos.posApps.Util;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pos.posApps.DTO.Dtos.LaporanPenjualanPerBarangDTO;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static java.awt.Color.LIGHT_GRAY;

@Component
public class LaporanPendapatanBarangPdfExporter {
    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DAY_RANGE =
            DateTimeFormatter.ofPattern("d MMMM yyyy", ID);

    public void export(
            List<LaporanPenjualanPerBarangDTO> rows,
            LocalDate startDate,
            LocalDate endDate,
            String supplierName,
            String query,
            OutputStream outputStream
    ) {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setFullCompression();
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph title = new Paragraph("Laporan Penjualan Per Barang", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph(
                    "Periode: " + startDate.format(DAY_RANGE) + " - " + endDate.format(DAY_RANGE)
            );
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            Paragraph supplierLine = new Paragraph(
                    "Supplier: " + (supplierName == null || supplierName.isBlank() ? "Semua" : supplierName)
            );
            supplierLine.setAlignment(Element.ALIGN_CENTER);
            document.add(supplierLine);

            if (query != null && !query.isBlank()) {
                Paragraph searchLine = new Paragraph("Pencarian: " + query.trim());
                searchLine.setAlignment(Element.ALIGN_CENTER);
                document.add(searchLine);
            }

            document.add(Chunk.NEWLINE);

            float[] columnWidths = {1, 5, 3, 2, 3, 3};
            String[] headers = {
                    "No",
                    "Produk",
                    "Supplier",
                    "Jumlah Terjual",
                    "Omzet",
                    "Laba"
            };
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(columnWidths);

            for (String header : headers) {
                table.addCell(createCell(header, headerFont, Element.ALIGN_CENTER, LIGHT_GRAY));
            }

            long totalQty = 0L;
            BigDecimal totalKotor = BigDecimal.ZERO;
            BigDecimal totalBersih = BigDecimal.ZERO;
            int index = 1;

            for (LaporanPenjualanPerBarangDTO row : rows) {
                long qty = row.getQty() == null ? 0L : row.getQty();
                BigDecimal kotor = row.getTotalHargaPenjualan() == null
                        ? BigDecimal.ZERO
                        : row.getTotalHargaPenjualan();
                BigDecimal bersih = row.getLabaPenjualan() == null
                        ? BigDecimal.ZERO
                        : row.getLabaPenjualan();
                totalQty += qty;
                totalKotor = totalKotor.add(kotor);
                totalBersih = totalBersih.add(bersih);

                table.addCell(createCell(String.valueOf(index), bodyFont, Element.ALIGN_CENTER, null));
                table.addCell(createCell(nullToEmpty(row.getProductName()), bodyFont, Element.ALIGN_LEFT, null));
                table.addCell(createCell(displaySupplier(row.getSupplierName()), bodyFont, Element.ALIGN_LEFT, null));
                table.addCell(createCell(formatQty(qty), bodyFont, Element.ALIGN_RIGHT, null));
                table.addCell(createCell(formatRupiah(kotor), bodyFont, Element.ALIGN_RIGHT, null));
                table.addCell(createCell(formatRupiah(bersih), bodyFont, Element.ALIGN_RIGHT, null));
                index++;
            }

            table.addCell(createCell("", totalFont, Element.ALIGN_CENTER, LIGHT_GRAY));
            table.addCell(createCell("Total", totalFont, Element.ALIGN_LEFT, LIGHT_GRAY));
            table.addCell(createCell("", totalFont, Element.ALIGN_LEFT, LIGHT_GRAY));
            table.addCell(createCell(formatQty(totalQty), totalFont, Element.ALIGN_RIGHT, LIGHT_GRAY));
            table.addCell(createCell(formatRupiah(totalKotor), totalFont, Element.ALIGN_RIGHT, LIGHT_GRAY));
            table.addCell(createCell(formatRupiah(totalBersih), totalFont, Element.ALIGN_RIGHT, LIGHT_GRAY));

            document.add(table);
            document.close();
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Gagal generate PDF streaming", e);
        }
    }

    private static String displaySupplier(String supplierName) {
        if (supplierName == null || supplierName.isBlank()) {
            return "-";
        }

        return supplierName;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
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

    private String formatQty(long qty) {
        return String.format(ID, "%,d", qty);
    }

    private String formatRupiah(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        return String.format("Rp %,.0f", amount).replace(",", ".");
    }
}
