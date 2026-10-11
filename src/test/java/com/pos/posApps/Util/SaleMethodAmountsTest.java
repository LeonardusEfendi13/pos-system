package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaleMethodAmountsTest {
    @Test
    void creditInvoiceStaysInTheCreditColumn() {
        SaleMethodAmounts.Totals totals = SaleMethodAmounts.allocate(
                false,
                new BigDecimal("100000"),
                "transfer",
                List.of(new SaleMethodAmounts.Line("transfer", new BigDecimal("100000")))
        );

        assertEquals(0, totals.credit().compareTo(new BigDecimal("100000")));
        assertEquals(0, totals.transfer().compareTo(BigDecimal.ZERO));
    }

    @Test
    void splitLinesKeepTheirOwnAmounts() {
        SaleMethodAmounts.Totals totals = SaleMethodAmounts.allocate(
                true,
                new BigDecimal("100000"),
                "cash",
                List.of(
                        new SaleMethodAmounts.Line("cash", new BigDecimal("60000")),
                        new SaleMethodAmounts.Line("qris", new BigDecimal("40000"))
                )
        );

        assertEquals(0, totals.cash().compareTo(new BigDecimal("60000")));
        assertEquals(0, totals.qris().compareTo(new BigDecimal("40000")));
        assertEquals(0, totals.transfer().compareTo(BigDecimal.ZERO));
        assertEquals(0, totals.credit().compareTo(BigDecimal.ZERO));
    }

    @Test
    void cashWithoutPaymentRowsUsesTheTransactionMethod() {
        SaleMethodAmounts.Totals totals = SaleMethodAmounts.allocate(
                true,
                new BigDecimal("15000"),
                "transfer",
                List.of()
        );

        assertEquals(0, totals.transfer().compareTo(new BigDecimal("15000")));
        assertEquals(0, totals.cash().compareTo(BigDecimal.ZERO));
    }
}
