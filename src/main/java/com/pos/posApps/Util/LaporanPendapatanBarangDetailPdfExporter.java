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
import com.pos.posApps.DTO.Dtos.LaporanPenjualanBarangBucketDTO;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static java.awt.Color.LIGHT_GRAY;

@Component
public class LaporanPendapatanBarangDetailPdfExporter {
    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DAY_RANGE =
            DateTimeFormatter.ofPattern("d MMMM yyyy", ID);
    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("d MMM yyyy", ID);
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMMM yyyy", ID);

    public void export(
            String productName,
            String supplierName,
            List<LaporanPenjualanBarangBucketDTO> rows,
            LocalDate startDate,
            LocalDate endDate,
            String filterOptions,
            OutputStream outputStream
    ) {
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setFullCompression();
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph title = new Paragraph("Laporan Penjualan Barang", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph productLine = new Paragraph(productName == null ? "" : productName);
            productLine.setAlignment(Element.ALIGN_CENTER);
            document.add(productLine);

            Paragraph supplierLine = new Paragraph(
                    "Supplier: " + (supplierName == null || supplierName.isBlank() ? "-" : supplierName)
            );
            supplierLine.setAlignment(Element.ALIGN_CENTER);
            document.add(supplierLine);

            Paragraph subtitle = new Paragraph(
                    "Periode: " + startDate.format(DAY_RANGE) + " - " + endDate.format(DAY_RANGE)
                            + " (" + filterLabel(filterOptions) + ")"
            );
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(Chunk.NEWLINE);

            float[] columnWidths = {4, 2, 3, 3};
            String[] headers = {"Periode", "Jumlah Terjual", "Omzet", "Laba"};
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(columnWidths);

            for (String header : headers) {
                table.addCell(createCell(header, headerFont, Element.ALIGN_CENTER, LIGHT_GRAY));
            }

            long totalQty = 0L;
            BigDecimal totalKotor = BigDecimal.ZERO;
            BigDecimal totalBersih = BigDecimal.ZERO;

            for (LaporanPenjualanBarangBucketDTO row : rows) {
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

                table.addCell(createCell(
                        formatPeriod(row.getPeriod(), filterOptions),
                        bodyFont,
                        Element.ALIGN_LEFT,
                        null
                ));
                table.addCell(createCell(formatQty(qty), bodyFont, Element.ALIGN_RIGHT, null));
                table.addCell(createCell(formatRupiah(kotor), bodyFont, Element.ALIGN_RIGHT, null));
                table.addCell(createCell(formatRupiah(bersih), bodyFont, Element.ALIGN_RIGHT, null));
            }

            table.addCell(createCell("Total", totalFont, Element.ALIGN_LEFT, LIGHT_GRAY));
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

    private static String filterLabel(String filterOptions) {
        String filter = LaporanPenjualanBarangView.normalizeFilter(filterOptions);
        return switch (filter) {
            case "month" -> "Bulan";
            case "year" -> "Tahun";
            default -> "Hari";
        };
    }

    private static String formatPeriod(String period, String filterOptions) {
        if (period == null || period.isBlank()) {
            return "";
        }

        String filter = LaporanPenjualanBarangView.normalizeFilter(filterOptions);
        try {
            if ("month".equals(filter) && period.length() == 7) {
                return YearMonth.parse(period).atDay(1).format(MONTH_LABEL);
            }

            if ("day".equals(filter) && period.length() == 10) {
                return LocalDate.parse(period).format(DAY_LABEL);
            }
        } catch (Exception ignored) {
            return period;
        }

        return period;
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
