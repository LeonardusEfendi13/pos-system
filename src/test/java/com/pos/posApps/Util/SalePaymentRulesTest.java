package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalePaymentRulesTest {

    @Test
    void cashShortOfTotalIsRejected() {
        SalePaymentRules.Decision decision = SalePaymentRules.resolve(
                false,
                true,
                new BigDecimal("5000"),
                new BigDecimal("10000"),
                null,
                null,
                null
        );

        assertFalse(decision.ok());
        assertEquals("Jumlah bayar kurang dari Grand Total.", decision.error());
    }

    @Test
    void cashStoresTheInvoiceTotalNotTheTender() {
        SalePaymentRules.Decision decision = SalePaymentRules.resolve(
                false,
                true,
                new BigDecimal("12000"),
                new BigDecimal("10000"),
                "2026-10-01",
                null,
                null
        );

        assertTrue(decision.ok());
        assertTrue(decision.cash());
        assertTrue(decision.paid());
        assertEquals(0, decision.paidAmount().compareTo(new BigDecimal("10000")));
        assertNull(decision.dueDate());
    }

    @Test
    void creditBalanceRequiresDueDateAndStoresTheDownPayment() {
        SalePaymentRules.Decision missingDue = SalePaymentRules.resolve(
                false,
                false,
                new BigDecimal("4000"),
                new BigDecimal("10000"),
                " ",
                null,
                null
        );
        SalePaymentRules.Decision decision = SalePaymentRules.resolve(
                false,
                false,
                new BigDecimal("4000"),
                new BigDecimal("10000"),
                "2026-10-01",
                null,
                null
        );

        assertFalse(missingDue.ok());
        assertEquals("Jatuh tempo wajib diisi.", missingDue.error());
        assertTrue(decision.ok());
        assertFalse(decision.cash());
        assertFalse(decision.paid());
        assertEquals(0, decision.paidAmount().compareTo(new BigDecimal("4000")));
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), decision.dueDate());
    }

    @Test
    void creditCoveredAtTheCounterIsPaidWithoutADueDate() {
        SalePaymentRules.Decision decision = SalePaymentRules.resolve(
                false,
                false,
                new BigDecimal("15000"),
                new BigDecimal("10000"),
                null,
                null,
                null
        );

        assertTrue(decision.ok());
        assertFalse(decision.cash());
        assertTrue(decision.paid());
        assertEquals(0, decision.paidAmount().compareTo(new BigDecimal("10000")));
        assertNull(decision.dueDate());
    }

    @Test
    void branchIgnoresCreditFlags() {
        SalePaymentRules.Decision decision = SalePaymentRules.resolve(
                true,
                false,
                BigDecimal.ZERO,
                new BigDecimal("8000"),
                null,
                null,
                null
        );

        assertTrue(decision.ok());
        assertTrue(decision.cash());
        assertTrue(decision.paid());
        assertEquals(0, decision.paidAmount().compareTo(new BigDecimal("8000")));
    }

    @Test
    void settledCreditStaysPaidWhenTheTenderIsLowered() {
        SalePaymentRules.Decision decision = SalePaymentRules.resolve(
                false,
                false,
                BigDecimal.ZERO,
                new BigDecimal("9000"),
                null,
                false,
                true
        );

        assertTrue(decision.ok());
        assertFalse(decision.cash());
        assertTrue(decision.paid());
        assertEquals(0, decision.paidAmount().compareTo(new BigDecimal("9000")));
        assertNull(decision.dueDate());
    }

    @Test
    void cashSwitchedToCreditOpensABalance() {
        SalePaymentRules.Decision decision = SalePaymentRules.resolve(
                false,
                false,
                new BigDecimal("1000"),
                new BigDecimal("9000"),
                "2026-10-02",
                true,
                true
        );

        assertTrue(decision.ok());
        assertFalse(decision.paid());
        assertEquals(0, decision.paidAmount().compareTo(new BigDecimal("1000")));
        assertEquals(LocalDateTime.of(2026, 10, 2, 0, 0), decision.dueDate());
    }

    @Test
    void missingCashFlagDefaultsToCash() {
        SalePaymentRules.Decision decision = SalePaymentRules.resolve(
                false,
                null,
                new BigDecimal("10000"),
                new BigDecimal("10000"),
                null,
                null,
                null
        );

        assertTrue(decision.cash());
        assertTrue(decision.paid());
    }
}
