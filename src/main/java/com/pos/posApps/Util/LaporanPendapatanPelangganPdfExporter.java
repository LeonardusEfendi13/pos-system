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
import com.pos.posApps.DTO.Dtos.LaporanPenjualanPerPelangganDTO;
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
public class LaporanPendapatanPelangganPdfExporter {
    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DAY_RANGE =
            DateTimeFormatter.ofPattern("d MMMM yyyy", ID);

    public void export(
            List<LaporanPenjualanPerPelangganDTO> rows,
            LocalDate startDate,
            LocalDate endDate,
            OutputStream outputStream) {
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setFullCompression();
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph title = new Paragraph("Laporan Pendapatan Per Pelanggan", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph(
                    "Periode: " + startDate.format(DAY_RANGE) + " - " + endDate.format(DAY_RANGE)
            );
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(Chunk.NEWLINE);

            float[] columnWidths = {1, 4, 4, 4};
            String[] headers = {
                    "No",
                    "Nama Pelanggan",
                    "Total Pendapatan Kotor",
                    "Total Pendapatan Bersih"
            };
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(columnWidths);

            for (String header : headers) {
                table.addCell(createCell(header, headerFont, Element.ALIGN_CENTER, LIGHT_GRAY));
            }

            int index = 1;
            for (LaporanPenjualanPerPelangganDTO row : rows) {
                table.addCell(createCell(String.valueOf(index), Element.ALIGN_CENTER));
                table.addCell(createCell(
                        row.getCustomerName() == null ? "" : row.getCustomerName(),
                        Element.ALIGN_LEFT
                ));
                table.addCell(createCell(formatRupiah(row.getTotalHargaPenjualan()), Element.ALIGN_RIGHT));
                table.addCell(createCell(formatRupiah(row.getLabaPenjualan()), Element.ALIGN_RIGHT));
                index++;
            }

            document.add(table);
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
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        return String.format("Rp %,.0f", amount).replace(",", ".");
    }
}
