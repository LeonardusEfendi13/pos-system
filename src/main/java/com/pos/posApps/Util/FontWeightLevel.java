package com.pos.posApps.Util;

public final class FontWeightLevel {

    public static final int DEFAULT = 2;
    public static final int MIN = 1;
    public static final int MAX = 4;

    private FontWeightLevel() {
    }

    public static boolean isValid(Integer level) {
        return level != null && level >= MIN && level <= MAX;
    }

    // A missing row or an out-of-range stored value reads as the default.
    public static int orDefault(Integer level) {
        return isValid(level) ? level : DEFAULT;
    }
}
