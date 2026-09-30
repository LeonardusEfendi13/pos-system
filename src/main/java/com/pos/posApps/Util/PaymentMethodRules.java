package com.pos.posApps.Util;

import java.util.Set;

public final class PaymentMethodRules {
    public static final Set<String> TYPES = Set.of("cash", "transfer", "credit", "qris");

    private PaymentMethodRules() {
    }

    public static String normalizeType(String methodType) {
        if (methodType == null) {
            return "";
        }

        return methodType.trim().toLowerCase();
    }

    public static String validate(
            String name,
            String methodType,
            String rekening,
            boolean systemDefault,
            String existingType
    ) {
        if (name == null || name.trim().isEmpty()) {
            return "Nama wajib diisi";
        }

        String type = normalizeType(methodType);
        if (!TYPES.contains(type)) {
            return "Tipe metode tidak valid";
        }

        if (systemDefault && existingType != null && !type.equals(normalizeType(existingType))) {
            return "Tipe metode bawaan tidak bisa diubah";
        }

        if ("transfer".equals(type) && (rekening == null || rekening.trim().isEmpty())) {
            return "Rekening wajib diisi";
        }

        return null;
    }

    public static String rekeningFor(String methodType, String rekening) {
        if (!"transfer".equals(normalizeType(methodType))) {
            return "";
        }

        return rekening == null ? "" : rekening.trim();
    }

    public static String deleteError(boolean systemDefault) {
        if (systemDefault) {
            return "Metode bawaan tidak bisa dihapus";
        }

        return null;
    }
}
