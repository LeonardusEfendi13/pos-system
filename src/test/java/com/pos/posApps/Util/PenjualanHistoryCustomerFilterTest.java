package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PenjualanHistoryCustomerFilterTest {

    @Test
    void withoutSelectionExcludesCabangIds() {
        PenjualanHistoryCustomerFilter.ResolvedType resolved =
                PenjualanHistoryCustomerFilter.resolve(null, List.of(10L, 20L));

        assertEquals(PenjualanHistoryCustomerFilter.Mode.EXCLUDE, resolved.mode());
        assertEquals(List.of(10L, 20L), resolved.customerIds());
    }

    @Test
    void withoutSelectionAndNoCabangKeepsAllSales() {
        PenjualanHistoryCustomerFilter.ResolvedType resolved =
                PenjualanHistoryCustomerFilter.resolve(null, List.of());

        assertEquals(PenjualanHistoryCustomerFilter.Mode.ALL, resolved.mode());
        assertTrue(resolved.customerIds().isEmpty());
    }

    @Test
    void selectedRegularCustomerIncludesOnlyThatId() {
        PenjualanHistoryCustomerFilter.ResolvedType resolved =
                PenjualanHistoryCustomerFilter.resolve(5L, List.of(10L, 20L));

        assertEquals(PenjualanHistoryCustomerFilter.Mode.INCLUDE, resolved.mode());
        assertEquals(List.of(5L), resolved.customerIds());
    }

    @Test
    void selectedCabangCustomerReturnsEmpty() {
        PenjualanHistoryCustomerFilter.ResolvedType resolved =
                PenjualanHistoryCustomerFilter.resolve(10L, List.of(10L, 20L));

        assertEquals(PenjualanHistoryCustomerFilter.Mode.EMPTY, resolved.mode());
        assertTrue(resolved.customerIds().isEmpty());
    }

    @Test
    void dropdownOmitsCabangCustomers() {
        List<Long> visible = PenjualanHistoryCustomerFilter.visibleCustomerIds(
                List.of(1L, 10L, 2L, 20L),
                List.of(10L, 20L)
        );

        assertEquals(List.of(1L, 2L), visible);
    }
}
