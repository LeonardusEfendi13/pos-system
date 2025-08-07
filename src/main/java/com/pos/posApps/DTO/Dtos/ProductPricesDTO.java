package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPricesDTO {
    private String productPricesId;
    private String productId;
//    private BigDecimal discount;
    private BigDecimal price;
    private Long minimalCount;

}
