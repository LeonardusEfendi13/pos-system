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

    @Test
    void hutangReportUsesItsOwnMenuKey() {
        assertEquals(
                List.of("laporan.penjualan.hutang"),
                MenuRoutePolicy.requiredKeys("GET", "/api/laporan/penjualan/hutang")
        );
        assertEquals(
                List.of("laporan.penjualan.hutang"),
                MenuRoutePolicy.requiredKeys("GET", "/api/laporan/penjualan/hutang/15")
        );
    }

    @Test
    void staffRoutesUseTheirOwnMenuKeys() {
        assertEquals(List.of("staff"), MenuRoutePolicy.requiredKeys("GET", "/api/staff"));
        assertEquals(List.of("staff"), MenuRoutePolicy.requiredKeys("PUT", "/api/staff/4"));
        assertEquals(List.of("staff.absensi"), MenuRoutePolicy.requiredKeys("PUT", "/api/staff/attendance"));
        assertEquals(List.of("staff.payroll"), MenuRoutePolicy.requiredKeys("POST", "/api/staff/payroll/preview"));
        assertEquals(List.of("staff.payroll"), MenuRoutePolicy.requiredKeys("POST", "/api/staff/payroll"));
    }

    @Test
    void priceListImportUsesTheDataCenterMenu() {
        assertEquals(List.of("data-center"), MenuRoutePolicy.requiredKeys("POST", "/api/data-center/price-list/preview"));
        assertEquals(List.of("data-center"), MenuRoutePolicy.requiredKeys("POST", "/api/data-center/price-list/confirm"));
    }
}
