package com.pos.posApps.Util;

import com.pos.posApps.DTO.Dtos.LaporanPaymentTypeDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentBreakdownTest {

    private static BigDecimal rp(long value) {
        return BigDecimal.valueOf(value);
    }

    @Test
    void groupsByTypeThenMethodWithSubtotals() {
        PaymentBreakdown breakdown = new PaymentBreakdown();
        breakdown.add("Transfer", "BCA", rp(3000));
        breakdown.add("Transfer", "Mandiri", rp(2000));
        breakdown.add("Transfer", "BCA", rp(500));
        breakdown.add("Cash", "Cash", rp(1000));

        List<LaporanPaymentTypeDTO> result = breakdown.build(List.of("Cash", "Transfer", "Kredit"));

        assertEquals(2, result.size());
        assertEquals("Cash", result.get(0).getTypeName());
        LaporanPaymentTypeDTO transfer = result.get(1);
        assertEquals(0, rp(5500).compareTo(transfer.getAmount()));
        assertEquals("BCA", transfer.getMethods().get(0).getMethodName());
        assertEquals(0, rp(3500).compareTo(transfer.getMethods().get(0).getAmount()));
        assertEquals("Mandiri", transfer.getMethods().get(1).getMethodName());
    }

    @Test
    void unknownTypesComeAfterKnownOnesByName() {
        PaymentBreakdown breakdown = new PaymentBreakdown();
        breakdown.add("Zeta Lama", "X", rp(1));
        breakdown.add("Alpha Lama", "Y", rp(1));
        breakdown.add("Kredit", "Kredit", rp(1));

        List<LaporanPaymentTypeDTO> result = breakdown.build(List.of("Cash", "Kredit"));

        assertEquals(List.of("Kredit", "Alpha Lama", "Zeta Lama"),
                result.stream().map(LaporanPaymentTypeDTO::getTypeName).toList());
    }

    @Test
    void skipsZeroAmountsAndDefaultsBlankNames() {
        PaymentBreakdown breakdown = new PaymentBreakdown();
        breakdown.add("QRIS", "Qris", BigDecimal.ZERO);
        breakdown.add(null, "", rp(700));

        List<LaporanPaymentTypeDTO> result = breakdown.build(List.of());

        assertEquals(1, result.size());
        assertEquals("Cash", result.get(0).getTypeName());
        assertEquals("Cash", result.get(0).getMethods().get(0).getMethodName());
        assertTrue(result.stream().noneMatch(type -> type.getTypeName().equals("QRIS")));
    }
}
