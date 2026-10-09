package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuRoutePolicyTest {
    @Test
    void productLookupIncludesKasir() {
        List<String> keys = MenuRoutePolicy.requiredKeys("GET", "/api/product/find");
        assertTrue(keys.contains("kasir"));
        assertTrue(keys.contains("products"));
    }

    @Test
    void userApiRequiresTheUserMenu() {
        assertEquals(List.of("user"), MenuRoutePolicy.requiredKeys("POST", "/api/user"));
        assertEquals(List.of("user"), MenuRoutePolicy.requiredKeys("DELETE", "/api/role/4"));
    }

    @Test
    void storeNameReadIsAvailableToEverySignedInUser() {
        assertTrue(MenuRoutePolicy.isShellRead("GET", "/api/client/settings"));
        assertFalse(MenuRoutePolicy.isShellRead("PUT", "/api/client/settings"));
        assertEquals(List.of("settings"), MenuRoutePolicy.requiredKeys("PUT", "/api/client/settings"));
    }

    @Test
    void preorderAddDoesNotUseTheHistoryKey() {
        assertEquals(List.of("preorder.tambah"), MenuRoutePolicy.requiredKeys("POST", "/api/preorder/add"));
        assertEquals(List.of("preorder.riwayat"), MenuRoutePolicy.requiredKeys("GET", "/api/preorder/15"));
    }
}
