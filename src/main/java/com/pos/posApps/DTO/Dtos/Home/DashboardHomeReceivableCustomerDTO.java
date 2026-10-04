package com.pos.posApps.DTO.Dtos.Home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardHomeReceivableCustomerDTO {
    private String name;
    private Long unpaidInvoiceCount;
    private BigDecimal unpaidTotal;
}
