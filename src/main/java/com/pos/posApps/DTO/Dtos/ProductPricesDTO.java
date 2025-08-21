package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPricesDTO {
    private Long productPricesId;
    private Long productId;
    private BigDecimal percentage;
    private BigDecimal price;
    private Long maximalCount;

}
