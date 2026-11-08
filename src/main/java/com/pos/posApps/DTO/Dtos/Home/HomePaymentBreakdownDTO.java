package com.pos.posApps.DTO.Dtos.Home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomePaymentBreakdownDTO {
    private String typeName;
    private String methodName;
    private BigDecimal amount;
    private Long count;
}
