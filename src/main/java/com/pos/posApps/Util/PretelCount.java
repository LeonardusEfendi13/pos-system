package com.pos.posApps.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PretelCount {
    private PretelCount() {
    }

    public record Line(long qty, long recipeQty) {
    }

    public record DraftLine(long productId, long qty, long recipeQty) {
    }

    public record RecipeComponent(long productId, long recipeQty) {
    }

    public record SaleLine(long productId, long qty) {
    }

    public record Movement(long productId, String tipe, long qtyIn, long qtyOut) {
    }

    public static int resolve(int clickCount, List<Line> lines) {
        if (clickCount <= 0 || lines == null || lines.isEmpty()) {
            return 0;
        }

        long needed = 0;
        for (Line line : lines) {
            if (line.qty() <= 0 || line.recipeQty() <= 0) {
                continue;
            }
            long sets = (line.qty() + line.recipeQty() - 1) / line.recipeQty();
            needed = Math.max(needed, sets);
        }

        if (needed <= 0) {
            return 0;
        }

        return (int) Math.min(clickCount, needed);
    }

    public static String validate(int clickCount, List<DraftLine> lines, Map<Long, Long> recipeQtyByProduct) {
        if (lines == null) {
            return null;
        }

        for (DraftLine line : lines) {
            Long recipeQty = recipeQtyByProduct.get(line.productId());
            if (recipeQty == null) {
                return "Komponen pretel tidak ada di resep";
            }
            if (line.recipeQty() != recipeQty) {
                return "Resep group set berubah. Hapus baris pretel lalu pretel lagi.";
            }
            if (line.qty() < 0) {
                return "Qty pretel tidak valid";
            }
            if (clickCount > 0 && line.qty() > recipeQty * clickCount) {
                return "Qty pretel melebihi jatah";
            }
        }

        return null;
    }

    public static boolean enoughSetStock(long stock, int pretelCount) {
        return pretelCount <= 0 || stock >= pretelCount;
    }

    public static List<Movement> movements(
            long parentProductId,
            int pretelCount,
            List<RecipeComponent> recipe,
            List<SaleLine> sales
    ) {
        List<Movement> result = new ArrayList<>();
        if (pretelCount > 0 && recipe != null) {
            List<RecipeComponent> ordered = recipe.stream()
                    .sorted(Comparator.comparingLong(RecipeComponent::productId))
                    .toList();
            for (RecipeComponent component : ordered) {
                result.add(new Movement(
                        component.productId(),
                        "PRETEL",
                        component.recipeQty() * pretelCount,
                        0
                ));
            }
            result.add(new Movement(parentProductId, "PRETEL", 0, pretelCount));
        }

        if (sales != null) {
            for (SaleLine sale : sales) {
                result.add(new Movement(sale.productId(), "PENJUALAN", 0, sale.qty()));
            }
        }

        return result;
    }
}
