package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSetComponentDTO {
    private Long productId;
    private String shortName;
    private String fullName;
    private Long qty;
    private Long stock;
    private BigDecimal hargaBeli;
    private List<ProductSetPriceDTO> prices;
}
