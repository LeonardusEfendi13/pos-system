package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuCatalogTest {
    @Test
    void matchesTheLongestHref() {
        assertEquals("branch.daftar", MenuCatalog.matchId("/branch"));
        assertEquals("branch.transfer.kasir", MenuCatalog.matchId("/branch/transfer/kasir"));
        assertEquals("penjualan", MenuCatalog.matchId("/penjualan/detail"));
        assertNull(MenuCatalog.matchId("/unknown"));
    }

    @Test
    void landsGodAdminOnUserAndSuperAdminOnHome() {
        assertEquals("/user", MenuCatalog.landingPath(true, false, List.of("user")));
        assertEquals("/home", MenuCatalog.landingPath(false, true, MenuCatalog.allKeys()));
        assertEquals("/kasir", MenuCatalog.landingPath(false, false, List.of("penjualan", "kasir")));
        assertEquals("/penjualan", MenuCatalog.landingPath(false, false, List.of("penjualan")));
        assertEquals(
                "/laporan/penjualan/periode",
                MenuCatalog.landingPath(false, false, List.of("laporan.penjualan", "laporan.penjualan.periode"))
        );
    }
}
