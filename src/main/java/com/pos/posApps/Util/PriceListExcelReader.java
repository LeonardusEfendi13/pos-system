package com.pos.posApps.Util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PriceListExcelReader {
    public static final int MAX_DATA_ROWS = 100_000;
    public static final String HEADER_ERROR = "Header harus berisi kode, nama, dan harga.";
    public static final String EXTRA_COLUMN_ERROR = "File hanya boleh berisi kolom kode, nama, dan harga.";
    public static final String EMPTY_FILE_ERROR = "File Excel kosong.";
    public static final String TOO_LARGE_ERROR = "File terlalu banyak barisnya.";

    private PriceListExcelReader() {
    }

    public record ParsedPriceRow(
            int excelRow,
            String kode,
            String nama,
            BigDecimal harga,
            String hargaText,
            String hargaError
    ) {
    }

    public record PriceListSheet(String error, List<ParsedPriceRow> rows) {
        public static PriceListSheet failed(String error) {
            return new PriceListSheet(error, List.of());
        }
    }

    public static PriceListSheet read(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                return PriceListSheet.failed(EMPTY_FILE_ERROR);
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return PriceListSheet.failed(EMPTY_FILE_ERROR);
            }
            DataFormatter formatter = new DataFormatter(Locale.US);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row header = sheet.getRow(0);
            if (header == null) {
                return PriceListSheet.failed(HEADER_ERROR);
            }
            int kodeCol = -1;
            int namaCol = -1;
            int hargaCol = -1;
            int used = 0;
            short lastCell = header.getLastCellNum();
            for (int column = 0; column < lastCell; column++) {
                String text = text(header.getCell(column), formatter, evaluator);
                if (text.isBlank()) {
                    continue;
                }
                used++;
                String key = normalizeHeader(text);
                if (isKode(key) && kodeCol < 0) {
                    kodeCol = column;
                } else if (isNama(key) && namaCol < 0) {
                    namaCol = column;
                } else if (isHarga(key) && hargaCol < 0) {
                    hargaCol = column;
                } else if (isKode(key) || isNama(key) || isHarga(key)) {
                    return PriceListSheet.failed(HEADER_ERROR);
                } else {
                    return PriceListSheet.failed(EXTRA_COLUMN_ERROR);
                }
            }
            if (used != 3 || kodeCol < 0 || namaCol < 0 || hargaCol < 0) {
                return PriceListSheet.failed(HEADER_ERROR);
            }

            List<ParsedPriceRow> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int index = 1; index <= lastRow; index++) {
                if (rows.size() >= MAX_DATA_ROWS) {
                    return PriceListSheet.failed(TOO_LARGE_ERROR);
                }
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                String kode = PriceListHargaParser.normalizeCode(
                        codeText(row.getCell(kodeCol), formatter, evaluator)
                );
                String nama = text(row.getCell(namaCol), formatter, evaluator).trim();
                Cell hargaCell = row.getCell(hargaCol);
                if (kode.isEmpty() && nama.isEmpty() && isBlank(hargaCell, formatter, evaluator)) {
                    continue;
                }
                ParsedHargaCell harga = readHarga(hargaCell, formatter, evaluator);
                rows.add(new ParsedPriceRow(
                        index + 1,
                        kode,
                        nama,
                        harga.value,
                        harga.text,
                        harga.error
                ));
            }
            return new PriceListSheet(null, rows);
        }
    }

    private record ParsedHargaCell(BigDecimal value, String text, String error) {
    }

    private static ParsedHargaCell readHarga(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (isBlank(cell, formatter, evaluator)) {
            return new ParsedHargaCell(null, "", PriceListHargaParser.EMPTY);
        }
        CellType type = effectiveType(cell, evaluator);
        if (type == CellType.NUMERIC) {
            BigDecimal value = PriceListHargaParser.canonical(
                    BigDecimal.valueOf(numeric(cell, evaluator))
            );
            String text = value.toPlainString();
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                return new ParsedHargaCell(null, text, PriceListHargaParser.NOT_POSITIVE);
            }
            return new ParsedHargaCell(value, text, null);
        }
        String text = text(cell, formatter, evaluator).trim();
        PriceListHargaParser.ParsedHarga parsed = PriceListHargaParser.parse(text);
        return new ParsedHargaCell(parsed.value(), text, parsed.error());
    }

    private static String codeText(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        CellType type = effectiveType(cell, evaluator);
        if (type == CellType.NUMERIC) {
            return PriceListHargaParser.canonical(
                    BigDecimal.valueOf(numeric(cell, evaluator))
            ).toPlainString();
        }
        return text(cell, formatter, evaluator);
    }

    private static boolean isBlank(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        return text(cell, formatter, evaluator).isBlank() && effectiveType(cell, evaluator) != CellType.NUMERIC;
    }

    private static CellType effectiveType(Cell cell, FormulaEvaluator evaluator) {
        if (cell.getCellType() == CellType.FORMULA) {
            return evaluator.evaluateFormulaCell(cell);
        }
        return cell.getCellType();
    }

    private static double numeric(Cell cell, FormulaEvaluator evaluator) {
        if (cell.getCellType() == CellType.FORMULA) {
            return evaluator.evaluate(cell).getNumberValue();
        }
        return cell.getNumericCellValue();
    }

    private static String text(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        String value = formatter.formatCellValue(cell, evaluator);
        return value == null ? "" : value.trim();
    }

    static String normalizeHeader(String header) {
        return header.toLowerCase(Locale.ROOT).replace('_', ' ').replaceAll("\\s+", " ").trim();
    }

    private static boolean isKode(String key) {
        return key.equals("kode") || key.equals("part number") || key.equals("partnumber");
    }

    private static boolean isNama(String key) {
        return key.equals("nama") || key.equals("description");
    }

    private static boolean isHarga(String key) {
        return key.equals("harga") || key.equals("het");
    }
}
