package com.pos.posApps.Util;

import java.util.Set;

public final class PaymentTypeRules {
    public static final Set<String> BEHAVIORS = Set.of("cash", "transfer", "credit", "qris");
    // Cash and credit have exactly one type each; checkout assumes it
    public static final Set<String> CREATABLE = Set.of("transfer", "qris");

    private PaymentTypeRules() {
    }

    public static String normalizeBehavior(String behavior) {
        if (behavior == null) {
            return "";
        }

        return behavior.trim().toLowerCase();
    }

    public static String validate(String name, String behavior, String existingBehavior) {
        if (name == null || name.trim().isEmpty()) {
            return "Nama wajib diisi";
        }

        String next = normalizeBehavior(behavior);
        if (!BEHAVIORS.contains(next)) {
            return "Perilaku tidak valid";
        }

        if (existingBehavior == null) {
            return CREATABLE.contains(next) ? null : "Perilaku hanya bisa Transfer atau Langsung lunas";
        }

        String current = normalizeBehavior(existingBehavior);
        if (!CREATABLE.contains(current) && !next.equals(current)) {
            return "Perilaku tipe bawaan tidak bisa diubah";
        }

        if (CREATABLE.contains(current) && !CREATABLE.contains(next)) {
            return "Perilaku hanya bisa Transfer atau Langsung lunas";
        }

        return null;
    }

    public static String deleteError(String behavior, boolean usedByMethods) {
        if (!CREATABLE.contains(normalizeBehavior(behavior))) {
            return "Tipe bawaan tidak bisa dihapus";
        }

        if (usedByMethods) {
            return "Tipe masih dipakai metode pembayaran";
        }

        return null;
    }
}
