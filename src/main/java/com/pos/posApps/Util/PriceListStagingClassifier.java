package com.pos.posApps.Util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PriceListStagingClassifier {
    public static final String READY = "READY";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String INVALID = "INVALID";
    public static final String CROSS_MERK = "CROSS_MERK";
    public static final String EMPTY_CODE = "Kode kosong";
    public static final String DUPLICATE_MESSAGE = "Duplikat kode, baris pertama yang dipakai";

    private PriceListStagingClassifier() {
    }

    public record StagedPriceRow(
            int excelRow,
            String partNumber,
            String nama,
            BigDecimal harga,
            String hargaText,
            String status,
            String errorMessage
    ) {
        public boolean blocksConfirm() {
            return INVALID.equals(status) || CROSS_MERK.equals(status);
        }
    }

    public static List<StagedPriceRow> classify(
            List<PriceListExcelReader.ParsedPriceRow> rows,
            String merk,
            Map<String, String> partMerk
    ) {
        List<StagedPriceRow> staged = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (PriceListExcelReader.ParsedPriceRow row : rows) {
            if (row.kode().isBlank()) {
                staged.add(stagedRow(row, INVALID, EMPTY_CODE));
                continue;
            }
            if (row.hargaError() != null) {
                staged.add(stagedRow(row, INVALID, row.hargaError()));
                continue;
            }
            if (!seen.add(row.kode())) {
                staged.add(stagedRow(row, DUPLICATE, DUPLICATE_MESSAGE));
                continue;
            }
            String owner = partMerk.get(row.kode());
            if (owner != null && !owner.equals(merk)) {
                staged.add(stagedRow(row, CROSS_MERK, "Kode sudah terdaftar di " + owner));
                continue;
            }
            staged.add(stagedRow(row, READY, null));
        }

        return staged;
    }

    private static StagedPriceRow stagedRow(
            PriceListExcelReader.ParsedPriceRow row,
            String status,
            String errorMessage
    ) {
        return new StagedPriceRow(
                row.excelRow(),
                row.kode(),
                row.nama(),
                row.harga(),
                row.hargaText(),
                status,
                errorMessage
        );
    }
}
