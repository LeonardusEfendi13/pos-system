package com.pos.posApps.Util;

import java.math.BigDecimal;
import java.util.List;

public final class SaleMethodAmounts {
    private SaleMethodAmounts() {
    }

    public record Line(String methodType, BigDecimal amount) {
    }

    public record Totals(
            BigDecimal cash,
            BigDecimal transfer,
            BigDecimal qris,
            BigDecimal credit
    ) {
        public static Totals zero() {
            return new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public static Totals allocate(
            boolean cashSale,
            BigDecimal totalPrice,
            String fallbackType,
            List<Line> lines
    ) {
        BigDecimal total = totalPrice == null ? BigDecimal.ZERO : totalPrice;
        if (!cashSale) {
            return new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, total);
        }

        if (lines != null && !lines.isEmpty()) {
            BigDecimal cash = BigDecimal.ZERO;
            BigDecimal transfer = BigDecimal.ZERO;
            BigDecimal qris = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;
            for (Line line : lines) {
                BigDecimal amount = line.amount() == null ? BigDecimal.ZERO : line.amount();
                switch (normalize(line.methodType())) {
                    case "transfer" -> transfer = transfer.add(amount);
                    case "qris" -> qris = qris.add(amount);
                    case "credit" -> credit = credit.add(amount);
                    default -> cash = cash.add(amount);
                }
            }
            return new Totals(cash, transfer, qris, credit);
        }

        return switch (normalize(fallbackType)) {
            case "transfer" -> new Totals(BigDecimal.ZERO, total, BigDecimal.ZERO, BigDecimal.ZERO);
            case "qris" -> new Totals(BigDecimal.ZERO, BigDecimal.ZERO, total, BigDecimal.ZERO);
            case "credit" -> new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, total);
            default -> new Totals(total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        };
    }

    private static String normalize(String methodType) {
        return methodType == null ? "" : methodType.trim().toLowerCase();
    }
}
