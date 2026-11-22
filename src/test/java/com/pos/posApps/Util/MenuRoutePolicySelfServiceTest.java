package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuRoutePolicySelfServiceTest {

    @Test
    void fontWeightUpdateIsSelfServiceForPutOnly() {
        assertTrue(MenuRoutePolicy.isSelfService("PUT", "/api/account/me/font-weight"));
        assertFalse(MenuRoutePolicy.isSelfService("GET", "/api/account/me/font-weight"));
        assertFalse(MenuRoutePolicy.isSelfService("PUT", "/api/account/other"));
    }
}
