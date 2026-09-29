package com.pos.posApps.Util;

import com.pos.posApps.DTO.Dtos.LaporanPenjualanBarangBucketDTO;
import com.pos.posApps.DTO.Dtos.LaporanPenjualanPerBarangDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LaporanPenjualanBarangView {
    public static final String DEFAULT_SORT = "qty";
    public static final String DEFAULT_DIR = "desc";

    private static final Set<String> SORTABLE = Set.of(
            "productName",
            "supplierName",
            "qty",
            "totalHargaPenjualan",
            "labaPenjualan"
    );

    private LaporanPenjualanBarangView() {
    }

    public static List<LaporanPenjualanPerBarangDTO> apply(
            List<LaporanPenjualanPerBarangDTO> rows,
            String q,
            String sort,
            String dir
    ) {
        List<LaporanPenjualanPerBarangDTO> filtered = filterByQuery(rows, q);
        return sortRows(filtered, sort, dir);
    }

    public static List<LaporanPenjualanBarangBucketDTO> fillBuckets(
            LocalDate startDate,
            LocalDate endDate,
            String filterOptions,
            Map<String, LaporanPenjualanBarangBucketDTO> found
    ) {
        String filter = normalizeFilter(filterOptions);
        List<String> periods = generatePeriods(startDate, endDate, filter);
        List<LaporanPenjualanBarangBucketDTO> buckets = new ArrayList<>(periods.size());

        for (String period : periods) {
            LaporanPenjualanBarangBucketDTO existing = found.get(period);
            if (existing != null) {
                buckets.add(existing);
                continue;
            }

            buckets.add(new LaporanPenjualanBarangBucketDTO(
                    period,
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            ));
        }

        return buckets;
    }

    public static String normalizeFilter(String filterOptions) {
        if (filterOptions == null) {
            return "day";
        }

        String filter = filterOptions.toLowerCase(Locale.ROOT);
        if ("month".equals(filter) || "year".equals(filter)) {
            return filter;
        }

        return "day";
    }

    public static String resolveSort(String sort) {
        if (sort != null && SORTABLE.contains(sort)) {
            return sort;
        }

        return DEFAULT_SORT;
    }

    public static String resolveDir(String sort, String dir) {
        if (sort == null || !SORTABLE.contains(sort)) {
            return DEFAULT_DIR;
        }

        if ("desc".equalsIgnoreCase(dir)) {
            return "desc";
        }

        return "asc";
    }

    private static List<LaporanPenjualanPerBarangDTO> filterByQuery(
            List<LaporanPenjualanPerBarangDTO> rows,
            String q
    ) {
        if (q == null || q.isBlank()) {
            return rows;
        }

        String needle = q.trim().toLowerCase(Locale.ROOT);
        return rows.stream()
                .filter(row -> contains(row.getProductName(), needle)
                        || contains(row.getShortName(), needle))
                .toList();
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static List<LaporanPenjualanPerBarangDTO> sortRows(
            List<LaporanPenjualanPerBarangDTO> rows,
            String sort,
            String dir
    ) {
        String resolvedSort = resolveSort(sort);
        boolean desc = "desc".equals(resolveDir(sort, dir));

        Comparator<LaporanPenjualanPerBarangDTO> comparator = switch (resolvedSort) {
            case "productName" -> Comparator.comparing(
                    row -> nullToEmpty(row.getProductName()),
                    String.CASE_INSENSITIVE_ORDER
            );
            case "supplierName" -> Comparator.comparing(
                    row -> nullToEmpty(row.getSupplierName()),
                    String.CASE_INSENSITIVE_ORDER
            );
            case "totalHargaPenjualan" -> Comparator.comparing(
                    row -> nullToZero(row.getTotalHargaPenjualan())
            );
            case "labaPenjualan" -> Comparator.comparing(
                    row -> nullToZero(row.getLabaPenjualan())
            );
            default -> Comparator.comparing(row -> nullToZero(row.getQty()));
        };

        if (desc) {
            comparator = comparator.reversed();
        }

        comparator = comparator.thenComparing(
                row -> row.getProductId() == null ? 0L : row.getProductId()
        );

        return rows.stream().sorted(comparator).toList();
    }

    private static List<String> generatePeriods(LocalDate startDate, LocalDate endDate, String filter) {
        List<String> periods = new ArrayList<>();

        switch (filter) {
            case "year" -> {
                Year startYear = Year.from(startDate);
                Year endYear = Year.from(endDate);
                for (Year year = startYear; !year.isAfter(endYear); year = year.plusYears(1)) {
                    periods.add(year.toString());
                }
            }
            case "month" -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                YearMonth startMonth = YearMonth.from(startDate);
                YearMonth endMonth = YearMonth.from(endDate);
                for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
                    periods.add(month.format(formatter));
                }
            }
            default -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    periods.add(date.format(formatter));
                }
            }
        }

        return periods;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
