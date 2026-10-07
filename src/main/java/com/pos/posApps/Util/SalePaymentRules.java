package com.pos.posApps.Util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class SalePaymentRules {
    private SalePaymentRules() {
    }

    public record Decision(
            boolean cash,
            boolean paid,
            BigDecimal paidAmount,
            LocalDateTime dueDate,
            String error
    ) {
        public boolean ok() {
            return error == null;
        }

        private static Decision failure(String error) {
            return new Decision(true, false, BigDecimal.ZERO, null, error);
        }

        private static Decision success(
                boolean cash,
                boolean paid,
                BigDecimal paidAmount,
                LocalDateTime dueDate
        ) {
            return new Decision(cash, paid, paidAmount, dueDate, null);
        }
    }

    public static Decision resolve(
            boolean branch,
            Boolean requestedCash,
            BigDecimal paymentAmount,
            BigDecimal totalPrice,
            String dueDate,
            Boolean existingCash,
            Boolean existingPaid,
            LocalDate invoiceDate,
            LocalDate today
    ) {
        return resolve(
                branch,
                requestedCash,
                paymentAmount,
                totalPrice,
                dueDate,
                existingCash,
                existingPaid,
                invoiceDate,
                today,
                false
        );
    }

    public static Decision resolve(
            boolean branch,
            Boolean requestedCash,
            BigDecimal paymentAmount,
            BigDecimal totalPrice,
            String dueDate,
            Boolean existingCash,
            Boolean existingPaid,
            LocalDate invoiceDate,
            LocalDate today,
            boolean automaticCreditDueDate
    ) {
        BigDecimal total = totalPrice == null ? BigDecimal.ZERO : totalPrice;

        if (branch) {
            return Decision.success(true, true, total, null);
        }

        boolean settledCredit = Boolean.FALSE.equals(existingCash) && Boolean.TRUE.equals(existingPaid);
        if (settledCredit) {
            boolean cash = requestedCash == null || requestedCash;
            return Decision.success(cash, true, total, null);
        }

        boolean cash = requestedCash == null || requestedCash;
        BigDecimal tender = paymentAmount == null ? BigDecimal.ZERO : paymentAmount;
        if (tender.signum() < 0) {
            return Decision.failure("Jumlah bayar tidak valid.");
        }

        if (cash) {
            if (tender.compareTo(total) < 0) {
                return Decision.failure("Jumlah bayar kurang dari Grand Total.");
            }
            return Decision.success(true, true, total, null);
        }

        if (automaticCreditDueDate) {
            if (tender.compareTo(total) > 0) {
                return Decision.failure("Uang muka tidak boleh melebihi Grand Total.");
            }

            LocalDate currentDay = today == null ? LocalDate.now() : today;
            return Decision.success(false, false, tender, currentDay.plusDays(14).atStartOfDay());
        }

        if (tender.compareTo(total) >= 0) {
            return Decision.success(false, true, total, null);
        }

        if (dueDate == null || dueDate.isBlank()) {
            return Decision.failure("Jatuh tempo wajib diisi.");
        }

        try {
            LocalDate parsed = LocalDate.parse(dueDate.trim());
            LocalDate currentDay = today == null ? LocalDate.now() : today;
            LocalDate invoice = invoiceDate == null ? currentDay : invoiceDate;
            if (!parsed.isAfter(currentDay) || !parsed.isAfter(invoice)) {
                return Decision.failure(
                        "Jatuh tempo harus setelah tanggal faktur dan setelah hari ini."
                );
            }
            return Decision.success(false, false, tender, parsed.atStartOfDay());
        } catch (DateTimeParseException exception) {
            return Decision.failure("Jatuh tempo tidak valid.");
        }
    }
}
