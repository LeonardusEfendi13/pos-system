package com.pos.posApps.DTO.Dtos.Home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeTopBarDTO {
    private Long countTransaction;
    private BigDecimal totalTransaction;
    private BigDecimal totalProfit;
}
