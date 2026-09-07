package com.pos.posApps.DTO.Dtos.Home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardHomeSupplierDTO {
    private String name;
    private Long invoiceCount;
    private BigDecimal totalSpending;
    private BigDecimal unpaidTotal;
}
