package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDetailDTO {
    private String code;
    private String name;
    private BigDecimal price;
    private Long qty;
    private BigDecimal discAmount;
    private BigDecimal total;
}
