package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HutangPenjualanRangeTest {
    @Test
    void blankDatesMeanAllTime() {
        HutangPenjualanRange.Bounds bounds = HutangPenjualanRange.resolve("  ", null);

        assertTrue(bounds.unbounded());
        assertEquals(0, bounds.applyStart());
        assertEquals(0, bounds.applyEnd());
        assertEquals("", bounds.startIso());
        assertEquals("", bounds.endIso());
    }

    @Test
    void aFilledRangeIsInclusiveThroughTheEndOfTheDay() {
        HutangPenjualanRange.Bounds bounds = HutangPenjualanRange.resolve(
                "2026-09-01",
                "2026-09-17"
        );

        assertEquals(1, bounds.applyStart());
        assertEquals(1, bounds.applyEnd());
        assertEquals(LocalDateTime.of(2026, 9, 1, 0, 0, 0), bounds.start());
        assertEquals(LocalDateTime.of(2026, 9, 17, 23, 59, 59), bounds.end());
        assertEquals("2026-09-01", bounds.startIso());
        assertEquals("2026-09-17", bounds.endIso());
    }

    @Test
    void oneDateAloneIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HutangPenjualanRange.resolve("2026-09-01", "")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> HutangPenjualanRange.resolve("", "2026-09-17")
        );
    }

    @Test
    void anEndBeforeTheStartIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HutangPenjualanRange.resolve("2026-09-17", "2026-09-01")
        );
    }
}
