package com.pos.posApps.Util;

import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.DTO.Dtos.PenjualanHistoryPaymentDTO;
import com.pos.posApps.DTO.Dtos.RecordedPaymentDTO;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class PenjualanPaymentPreview {
    private PenjualanPaymentPreview() {
    }

    public static List<PenjualanHistoryPaymentDTO> lines(PenjualanDTO penjualan) {
        if (penjualan == null) {
            return List.of();
        }

        BigDecimal total = penjualan.getTotalPrice() == null ? BigDecimal.ZERO : penjualan.getTotalPrice();
        if (!penjualan.isCash()) {
            return List.of(new PenjualanHistoryPaymentDTO("Kredit", total));
        }

        List<RecordedPaymentDTO> payments = penjualan.getPayments();
        if (payments != null && !payments.isEmpty()) {
            return payments.stream()
                    .sorted(Comparator.comparing(row -> row.getSortOrder() == null ? 0 : row.getSortOrder()))
                    .map(row -> new PenjualanHistoryPaymentDTO(label(row.getPaymentMethodName()), amount(row.getAmount())))
                    .toList();
        }

        return List.of(new PenjualanHistoryPaymentDTO(label(penjualan.getPaymentMethodName()), total));
    }

    private static String label(String name) {
        if (name == null || name.isBlank()) {
            return "Cash";
        }

        return name;
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
