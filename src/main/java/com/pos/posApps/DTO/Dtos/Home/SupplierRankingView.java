package com.pos.posApps.DTO.Dtos.Home;

import java.math.BigDecimal;

public interface SupplierRankingView {
    String getName();

    Long getInvoiceCount();

    BigDecimal getTotalSpending();

    BigDecimal getUnpaidTotal();
}
