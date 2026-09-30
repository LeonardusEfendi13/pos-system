package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalePaymentRulesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);
    private static final LocalDate INVOICE = LocalDate.of(2026, 9, 30);

    @Test
    void cashShortOfTotalIsRejected() {
        SalePaymentRules.Decision decision = SalePaymentRules.resolve(
                false,
                true,
                new BigDecimal("5000"),
                new BigDecimal("10000"),
                null,
                null,
                null,
                INVOICE,
                TODAY
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
                null,
                INVOICE,
                TODAY
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
                null,
                INVOICE,
                TODAY
        );
        SalePaymentRules.Decision decision = SalePaymentRules.resolve(
                false,
                false,
                new BigDecimal("4000"),
                new BigDecimal("10000"),
                "2026-10-01",
                null,
                null,
                INVOICE,
                TODAY
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
                null,
                INVOICE,
                TODAY
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
                null,
                INVOICE,
                TODAY
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
                true,
                INVOICE,
                TODAY
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
                true,
                INVOICE,
                TODAY
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
                null,
                INVOICE,
                TODAY
        );

        assertTrue(decision.cash());
        assertTrue(decision.paid());
    }

    @Test
    void dueDateOnOrBeforeTodayOrInvoiceIsRejected() {
        SalePaymentRules.Decision todayDue = SalePaymentRules.resolve(
                false,
                false,
                new BigDecimal("1000"),
                new BigDecimal("9000"),
                "2026-09-30",
                null,
                null,
                INVOICE,
                TODAY
        );
        SalePaymentRules.Decision oldInvoiceYesterday = SalePaymentRules.resolve(
                false,
                false,
                new BigDecimal("1000"),
                new BigDecimal("9000"),
                "2026-09-29",
                null,
                null,
                LocalDate.of(2026, 9, 1),
                TODAY
        );
        SalePaymentRules.Decision afterBoth = SalePaymentRules.resolve(
                false,
                false,
                new BigDecimal("1000"),
                new BigDecimal("9000"),
                "2026-10-01",
                null,
                null,
                LocalDate.of(2026, 9, 1),
                TODAY
        );

        assertFalse(todayDue.ok());
        assertEquals(
                "Jatuh tempo harus setelah tanggal faktur dan setelah hari ini.",
                todayDue.error()
        );
        assertFalse(oldInvoiceYesterday.ok());
        assertTrue(afterBoth.ok());
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), afterBoth.dueDate());
    }
}
