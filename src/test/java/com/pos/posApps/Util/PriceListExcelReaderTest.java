package com.pos.posApps.Util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PriceListExcelReaderTest {
    @Test
    void readsTemplateHeadersInAnyOrderAndSkipsEmptyRows() throws Exception {
        byte[] file = workbook((sheet) -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("HET");
            header.createCell(1).setCellValue("Description");
            header.createCell(2).setCellValue("Part Number");
            Row data = sheet.createRow(1);
            data.createCell(2).setCellValue("abc-1");
            data.createCell(1).setCellValue("Filter");
            data.createCell(0).setCellValue("1.250.000");
            sheet.createRow(2);
        });

        PriceListExcelReader.PriceListSheet sheet = PriceListExcelReader.read(new ByteArrayInputStream(file));

        assertNull(sheet.error());
        assertEquals(1, sheet.rows().size());
        assertEquals("ABC-1", sheet.rows().get(0).kode());
        assertEquals("Filter", sheet.rows().get(0).nama());
        assertEquals(new BigDecimal("1250000"), sheet.rows().get(0).harga());
        assertEquals(2, sheet.rows().get(0).excelRow());
    }

    @Test
    void rejectsAnExtraHeader() throws Exception {
        byte[] file = workbook((sheet) -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("kode");
            header.createCell(1).setCellValue("nama");
            header.createCell(2).setCellValue("harga");
            header.createCell(3).setCellValue("stok");
        });

        PriceListExcelReader.PriceListSheet sheet = PriceListExcelReader.read(new ByteArrayInputStream(file));

        assertEquals(PriceListExcelReader.EXTRA_COLUMN_ERROR, sheet.error());
    }

    @Test
    void keepsTheFirstCodeAndBlocksOtherMerks() {
        List<PriceListExcelReader.ParsedPriceRow> rows = List.of(
                new PriceListExcelReader.ParsedPriceRow(2, "AAA", "Satu", new BigDecimal("10"), "10", null),
                new PriceListExcelReader.ParsedPriceRow(3, "AAA", "Duplikat", new BigDecimal("11"), "11", null),
                new PriceListExcelReader.ParsedPriceRow(4, "BBB", "Honda", new BigDecimal("12"), "12", null),
                new PriceListExcelReader.ParsedPriceRow(5, "", "Tanpa kode", null, "", PriceListHargaParser.EMPTY),
                new PriceListExcelReader.ParsedPriceRow(6, "CCC", "Nol", null, "0", PriceListHargaParser.NOT_POSITIVE)
        );

        List<PriceListStagingClassifier.StagedPriceRow> staged = PriceListStagingClassifier.classify(
                rows,
                "YAMAHA",
                Map.of("BBB", "HONDA")
        );

        assertEquals(PriceListStagingClassifier.READY, staged.get(0).status());
        assertEquals(PriceListStagingClassifier.DUPLICATE, staged.get(1).status());
        assertEquals(PriceListStagingClassifier.CROSS_MERK, staged.get(2).status());
        assertEquals("Kode sudah terdaftar di HONDA", staged.get(2).errorMessage());
        assertEquals(PriceListStagingClassifier.INVALID, staged.get(3).status());
        assertEquals(PriceListStagingClassifier.INVALID, staged.get(4).status());
    }

    private byte[] workbook(SheetWriter writer) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writer.write(workbook.createSheet("harga"));
            workbook.write(output);
            return output.toByteArray();
        }
    }

    @FunctionalInterface
    private interface SheetWriter {
        void write(org.apache.poi.ss.usermodel.Sheet sheet);
    }
}
