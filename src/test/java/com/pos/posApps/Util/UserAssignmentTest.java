package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAssignmentTest {
    @Test
    void godAdminCreateIsAlwaysSuperAdmin() {
        assertEquals(UserAssignment.Kind.SUPER_ADMIN, UserAssignment.create(true, false));
        assertEquals(UserAssignment.Kind.SUPER_ADMIN, UserAssignment.create(true, true));
    }

    @Test
    void otherActorsNeedAnOperationalRole() {
        assertEquals(UserAssignment.Kind.OPERATIONAL, UserAssignment.create(false, true));
        assertEquals(UserAssignment.Kind.INVALID, UserAssignment.create(false, false));
    }

    @Test
    void rejectsReservedNames() {
        assertTrue(UserAssignment.reservedRoleName("SUPER_ADMIN"));
        assertTrue(UserAssignment.reservedRoleName("god admin"));
        assertFalse(UserAssignment.reservedRoleName("Kasir"));
    }
}
