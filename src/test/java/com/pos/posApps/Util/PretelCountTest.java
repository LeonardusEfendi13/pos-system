package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PretelCountTest {

    @Test
    void unchangedCountsNeedNoWork() {
        assertTrue(PretelCount.changedCounts(Map.of(10L, 1L), Map.of(10L, 1L)).isEmpty());
    }

    @Test
    void removedPretelIsReassembledAndNewOneApplied() {
        List<PretelCount.CountChange> changes = PretelCount.changedCounts(
                Map.of(10L, 1L, 20L, 2L),
                Map.of(20L, 3L, 30L, 1L)
        );

        assertEquals(List.of(
                new PretelCount.CountChange(10L, 1L, 0L),
                new PretelCount.CountChange(20L, 2L, 3L),
                new PretelCount.CountChange(30L, 0L, 1L)
        ), changes);
    }

    @Test
    void sumsTheSameProductAcrossRows() {
        Map<Long, Long> sums = PretelCount.sumQtyByProduct(List.of(
                new PretelCount.SaleLine(1L, 2L),
                new PretelCount.SaleLine(2L, 1L),
                new PretelCount.SaleLine(1L, 1L)
        ));

        assertEquals(Map.of(1L, 3L, 2L, 1L), sums);
    }
}
