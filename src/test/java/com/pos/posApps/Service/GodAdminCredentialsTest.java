package com.pos.posApps.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GodAdminCredentialsTest {
    @Test
    void acceptsOnlyTheEnvPasswordForTheBootstrapUsername() {
        GodAdminCredentials credentials = new GodAdminCredentials("bootstrap", "secret");

        assertTrue(credentials.matchesUsername("bootstrap"));
        assertTrue(credentials.passwordMatches("secret"));
        assertFalse(credentials.passwordMatches("database-password"));
        assertFalse(credentials.matchesUsername("other"));
    }

    @Test
    void staysDisabledWhenEnvIsEmpty() {
        GodAdminCredentials credentials = new GodAdminCredentials("", "");

        assertFalse(credentials.isConfigured());
        assertFalse(credentials.matchesUsername("bootstrap"));
        assertFalse(credentials.passwordMatches("secret"));
    }
}
