package com.pos.posApps.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class GodAdminCredentials {
    private final String username;
    private final String password;

    public GodAdminCredentials(String username, String password) {
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
    }

    public boolean isConfigured() {
        return !username.isBlank() && !password.isBlank();
    }

    public String username() {
        return username;
    }

    public boolean matchesUsername(String value) {
        if (!isConfigured() || value == null) {
            return false;
        }
        return username.equals(value.trim());
    }

    public boolean passwordMatches(String raw) {
        if (!isConfigured() || raw == null) {
            return false;
        }
        byte[] expected = password.getBytes(StandardCharsets.UTF_8);
        byte[] actual = raw.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
