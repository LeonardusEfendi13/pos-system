package com.pos.posApps.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PenjualanHistoryCustomerFilter {
    public enum Mode {
        ALL,
        INCLUDE,
        EXCLUDE,
        EMPTY
    }

    public record ResolvedType(Mode mode, List<Long> customerIds) {
    }

    private PenjualanHistoryCustomerFilter() {
    }

    public static List<Long> cabangIdsOf(List<Long> cabangCustomerIds) {
        if (cabangCustomerIds == null || cabangCustomerIds.isEmpty()) {
            return List.of();
        }

        return cabangCustomerIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    public static ResolvedType resolve(Long selectedCustomerId, List<Long> cabangCustomerIds) {
        List<Long> cabangIds = cabangIdsOf(cabangCustomerIds);

        if (selectedCustomerId != null) {
            if (cabangIds.contains(selectedCustomerId)) {
                return new ResolvedType(Mode.EMPTY, List.of());
            }
            return new ResolvedType(Mode.INCLUDE, List.of(selectedCustomerId));
        }

        if (cabangIds.isEmpty()) {
            return new ResolvedType(Mode.ALL, List.of());
        }

        return new ResolvedType(Mode.EXCLUDE, cabangIds);
    }

    public static List<Long> visibleCustomerIds(List<Long> allCustomerIds, List<Long> cabangCustomerIds) {
        Set<Long> cabangIds = new HashSet<>(cabangIdsOf(cabangCustomerIds));
        if (allCustomerIds == null || allCustomerIds.isEmpty()) {
            return List.of();
        }

        List<Long> visible = new ArrayList<>();
        for (Long customerId : allCustomerIds) {
            if (customerId != null && !cabangIds.contains(customerId)) {
                visible.add(customerId);
            }
        }
        return visible;
    }
}
