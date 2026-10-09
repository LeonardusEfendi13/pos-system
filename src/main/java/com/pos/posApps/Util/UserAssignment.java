package com.pos.posApps.Util;

public final class UserAssignment {
    public enum Kind {
        SUPER_ADMIN,
        OPERATIONAL,
        INVALID
    }

    private UserAssignment() {
    }

    public static Kind create(boolean godAdmin, boolean operationalRoleFound) {
        if (godAdmin) {
            return Kind.SUPER_ADMIN;
        }
        if (!operationalRoleFound) {
            return Kind.INVALID;
        }
        return Kind.OPERATIONAL;
    }

    public static boolean reservedRoleName(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.trim();
        return normalized.equalsIgnoreCase("SUPER_ADMIN")
                || normalized.equalsIgnoreCase("GOD_ADMIN")
                || normalized.equalsIgnoreCase("Super Admin")
                || normalized.equalsIgnoreCase("God Admin");
    }
}
