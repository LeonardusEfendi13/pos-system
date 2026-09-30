package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PretelCountTest {

    @Test
    void loweringQtyKeepsOneFullSet() {
        int count = PretelCount.resolve(2, List.of(new PretelCount.Line(1, 1)));

        assertEquals(1, count);
    }

    @Test
    void partialComponentQtyStillPretelsTheWholeSet() {
        int count = PretelCount.resolve(1, List.of(new PretelCount.Line(1, 2)));

        assertEquals(1, count);
    }

    @Test
    void deletedLinesPretelNothing() {
        assertEquals(0, PretelCount.resolve(2, List.of()));
        assertEquals(0, PretelCount.resolve(2, List.of(new PretelCount.Line(0, 1))));
    }

    @Test
    void twoComponentsUseTheHigherSetNeed() {
        int count = PretelCount.resolve(3, List.of(
                new PretelCount.Line(2, 1),
                new PretelCount.Line(2, 2)
        ));

        assertEquals(2, count);
    }

    @Test
    void qtyAboveAllocationIsRejected() {
        String error = PretelCount.validate(
                1,
                List.of(new PretelCount.DraftLine(10, 3, 1)),
                Map.of(10L, 1L)
        );

        assertEquals("Qty pretel melebihi jatah", error);
    }

    @Test
    void changedRecipeIsRejected() {
        String error = PretelCount.validate(
                1,
                List.of(new PretelCount.DraftLine(10, 1, 1)),
                Map.of(10L, 2L)
        );

        assertEquals("Resep barang set berubah. Hapus baris pretel lalu pretel lagi.", error);
    }

    @Test
    void matchingRecipeIsAccepted() {
        assertNull(PretelCount.validate(
                1,
                List.of(new PretelCount.DraftLine(10, 1, 2)),
                Map.of(10L, 2L)
        ));
    }

    @Test
    void movementsIncreaseEveryChildBeforeTheSale() {
        List<PretelCount.Movement> movements = PretelCount.movements(
                1,
                1,
                List.of(
                        new PretelCount.RecipeComponent(30, 2),
                        new PretelCount.RecipeComponent(20, 1)
                ),
                List.of(new PretelCount.SaleLine(20, 1))
        );

        assertEquals(List.of(
                new PretelCount.Movement(20, "PRETEL", 1, 0),
                new PretelCount.Movement(30, "PRETEL", 2, 0),
                new PretelCount.Movement(1, "PRETEL", 0, 1),
                new PretelCount.Movement(20, "PENJUALAN", 0, 1)
        ), movements);
        assertTrue(PretelCount.enoughSetStock(1, 1));
        assertTrue(!PretelCount.enoughSetStock(0, 1));
    }
}
