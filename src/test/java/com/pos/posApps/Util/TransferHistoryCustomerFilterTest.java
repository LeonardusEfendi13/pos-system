package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferHistoryCustomerFilterTest {

    @Test
    void withoutSelectionIncludesEveryCabang() {
        assertEquals(
                List.of(10L, 20L),
                TransferHistoryCustomerFilter.includeIds(null, List.of(10L, 20L))
        );
    }

    @Test
    void selectedCabangIncludesOnlyThatId() {
        assertEquals(
                List.of(20L),
                TransferHistoryCustomerFilter.includeIds(20L, List.of(10L, 20L))
        );
    }

    @Test
    void selectedNonCabangReturnsEmpty() {
        assertTrue(TransferHistoryCustomerFilter.includeIds(5L, List.of(10L, 20L)).isEmpty());
    }

    @Test
    void emptyCabangListReturnsEmpty() {
        assertTrue(TransferHistoryCustomerFilter.includeIds(null, List.of()).isEmpty());
    }
}
