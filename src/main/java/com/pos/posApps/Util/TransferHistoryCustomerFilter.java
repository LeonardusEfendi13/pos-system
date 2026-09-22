package com.pos.posApps.Util;

import java.util.List;

public final class TransferHistoryCustomerFilter {
    private TransferHistoryCustomerFilter() {
    }

    public static List<Long> includeIds(Long selectedCustomerId, List<Long> cabangCustomerIds) {
        List<Long> cabangIds = PenjualanHistoryCustomerFilter.cabangIdsOf(cabangCustomerIds);

        if (cabangIds.isEmpty()) {
            return List.of();
        }

        if (selectedCustomerId == null) {
            return cabangIds;
        }

        if (cabangIds.contains(selectedCustomerId)) {
            return List.of(selectedCustomerId);
        }

        return List.of();
    }
}
