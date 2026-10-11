package com.pos.posApps.Util;

import java.math.BigDecimal;
import java.util.Locale;

public final class PriceListHargaParser {
    public static final String EMPTY = "Harga kosong";
    public static final String NOT_A_NUMBER = "Harga bukan angka";
    public static final String NOT_POSITIVE = "Harga harus lebih dari 0";

    private PriceListHargaParser() {
    }

    public record ParsedHarga(BigDecimal value, String error) {
    }

    public static ParsedHarga parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedHarga(null, EMPTY);
        }

        String normalized = normalize(raw);
        if (normalized.isEmpty()) {
            return new ParsedHarga(null, NOT_A_NUMBER);
        }

        String plain = toPlainNumber(normalized);
        if (plain == null) {
            return new ParsedHarga(null, NOT_A_NUMBER);
        }

        BigDecimal value = new BigDecimal(plain);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return new ParsedHarga(null, NOT_POSITIVE);
        }

        return new ParsedHarga(canonical(value), null);
    }

    private static String normalize(String raw) {
        String value = raw.replace('\u00A0', ' ').replace(" ", "").trim();
        if (value.length() >= 2 && value.regionMatches(true, 0, "rp", 0, 2)) {
            value = value.substring(2);
            if (value.startsWith(".")) {
                value = value.substring(1);
            }
        }
        return value;
    }

    private static String toPlainNumber(String value) {
        boolean hasDot = value.indexOf('.') >= 0;
        boolean hasComma = value.indexOf(',') >= 0;
        String plain;

        if (hasDot && hasComma) {
            if (!value.matches("\\d{1,3}(\\.\\d{3})+,\\d+")) {
                return null;
            }
            plain = value.replace(".", "").replace(',', '.');
        } else if (hasComma) {
            if (!value.matches("\\d+,\\d+")) {
                return null;
            }
            plain = value.replace(',', '.');
        } else if (hasDot) {
            if (!value.matches("\\d{1,3}(\\.\\d{3})+")) {
                return null;
            }
            plain = value.replace(".", "");
        } else if (value.matches("\\d+")) {
            plain = value;
        } else {
            return null;
        }

        return plain;
    }

    public static BigDecimal canonical(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            return stripped.setScale(0);
        }
        return stripped;
    }

    public static String display(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    public static String normalizeCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
