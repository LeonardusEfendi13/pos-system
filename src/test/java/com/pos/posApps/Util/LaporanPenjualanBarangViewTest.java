package com.pos.posApps.Util;

import com.pos.posApps.DTO.Dtos.LaporanPenjualanBarangBucketDTO;
import com.pos.posApps.DTO.Dtos.LaporanPenjualanPerBarangDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LaporanPenjualanBarangViewTest {

    @Test
    void defaultSortIsQtyDescending() {
        List<LaporanPenjualanPerBarangDTO> sorted = LaporanPenjualanBarangView.apply(
                List.of(row(1L, "Anting", "ANT", "A", 10), row(2L, "Kabel", "KAB", "B", 3)),
                null,
                null,
                null
        );

        assertEquals(1L, sorted.get(0).getProductId());
        assertEquals(2L, sorted.get(1).getProductId());
    }

    @Test
    void searchMatchesShortNameAndFullName() {
        List<LaporanPenjualanPerBarangDTO> rows = List.of(
                row(1L, "Bos Klep", "BOS", "Sinar", 9),
                row(2L, "Kabel Tis", "KAB", "Sinar", 4)
        );

        List<LaporanPenjualanPerBarangDTO> byShort = LaporanPenjualanBarangView.apply(rows, "kab", null, null);
        List<LaporanPenjualanPerBarangDTO> byFull = LaporanPenjualanBarangView.apply(rows, "klep", null, null);

        assertEquals(1, byShort.size());
        assertEquals(2L, byShort.get(0).getProductId());
        assertEquals(1, byFull.size());
        assertEquals(1L, byFull.get(0).getProductId());
    }

    @Test
    void zeroQtyRowsStayOutWhenCallerAlreadyDroppedThem() {
        List<LaporanPenjualanPerBarangDTO> sorted = LaporanPenjualanBarangView.apply(
                List.of(row(1L, "Bos", "BOS", "", 2)),
                "   ",
                "qty",
                "asc"
        );

        assertEquals(1, sorted.size());
        assertEquals("asc", LaporanPenjualanBarangView.resolveDir("qty", "asc"));
    }

    @Test
    void dayBucketsFillMissingDatesWithZero() {
        List<LaporanPenjualanBarangBucketDTO> buckets = LaporanPenjualanBarangView.fillBuckets(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 3),
                "day",
                Map.of(
                        "2026-09-02",
                        new LaporanPenjualanBarangBucketDTO(
                                "2026-09-02",
                                4L,
                                new BigDecimal("10000"),
                                new BigDecimal("2000")
                        )
                )
        );

        assertEquals(List.of("2026-09-01", "2026-09-02", "2026-09-03"),
                buckets.stream().map(LaporanPenjualanBarangBucketDTO::getPeriod).toList());
        assertEquals(0L, buckets.get(0).getQty());
        assertEquals(4L, buckets.get(1).getQty());
        assertEquals(0L, buckets.get(2).getQty());
        assertEquals(BigDecimal.ZERO, buckets.get(0).getTotalHargaPenjualan());
    }

    private static LaporanPenjualanPerBarangDTO row(
            Long productId,
            String productName,
            String shortName,
            String supplierName,
            long qty
    ) {
        return new LaporanPenjualanPerBarangDTO(
                productId,
                productName,
                shortName,
                supplierName,
                qty,
                BigDecimal.valueOf(qty * 1000),
                BigDecimal.valueOf(qty * 100)
        );
    }
}
