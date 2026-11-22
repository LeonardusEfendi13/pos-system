package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontWeightLevelTest {

    @Test
    void acceptsOnlyLevelsOneToFour() {
        assertTrue(FontWeightLevel.isValid(1));
        assertTrue(FontWeightLevel.isValid(4));
        assertFalse(FontWeightLevel.isValid(0));
        assertFalse(FontWeightLevel.isValid(5));
        assertFalse(FontWeightLevel.isValid(null));
    }

    @Test
    void missingOrInvalidStoredValueReadsAsDefault() {
        assertEquals(2, FontWeightLevel.orDefault(null));
        assertEquals(2, FontWeightLevel.orDefault(9));
        assertEquals(3, FontWeightLevel.orDefault(3));
    }
}
