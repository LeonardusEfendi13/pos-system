package com.pos.posApps.Util;

import com.pos.posApps.DTO.Dtos.LaporanPaymentMethodAmountDTO;
import com.pos.posApps.DTO.Dtos.LaporanPaymentTypeDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Groups report payment amounts by type name, then method name
public final class PaymentBreakdown {
    private final Map<String, Map<String, BigDecimal>> amounts = new LinkedHashMap<>();

    public void add(String typeName, String methodName, BigDecimal amount) {
        if (amount == null || amount.signum() == 0) {
            return;
        }

        String type = blankToDefault(typeName, "Cash");
        String method = blankToDefault(methodName, type);
        amounts.computeIfAbsent(type, key -> new LinkedHashMap<>())
                .merge(method, amount, BigDecimal::add);
    }

    // Types follow typeOrder; names not in it (deleted or renamed types) come after, by name.
    // Methods are sorted by amount, largest first.
    public List<LaporanPaymentTypeDTO> build(List<String> typeOrder) {
        List<LaporanPaymentTypeDTO> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> type : amounts.entrySet()) {
            List<LaporanPaymentMethodAmountDTO> methods = type.getValue().entrySet().stream()
                    .filter(entry -> entry.getValue().signum() != 0)
                    .map(entry -> new LaporanPaymentMethodAmountDTO(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(LaporanPaymentMethodAmountDTO::getAmount).reversed()
                            .thenComparing(LaporanPaymentMethodAmountDTO::getMethodName))
                    .toList();
            if (methods.isEmpty()) {
                continue;
            }

            BigDecimal total = methods.stream()
                    .map(LaporanPaymentMethodAmountDTO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new LaporanPaymentTypeDTO(type.getKey(), total, methods));
        }

        result.sort(Comparator
                .comparingInt((LaporanPaymentTypeDTO dto) -> orderOf(typeOrder, dto.getTypeName()))
                .thenComparing(LaporanPaymentTypeDTO::getTypeName));
        return result;
    }

    private static int orderOf(List<String> typeOrder, String name) {
        for (int i = 0; i < typeOrder.size(); i++) {
            if (typeOrder.get(i).equalsIgnoreCase(name)) {
                return i;
            }
        }

        return Integer.MAX_VALUE;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
