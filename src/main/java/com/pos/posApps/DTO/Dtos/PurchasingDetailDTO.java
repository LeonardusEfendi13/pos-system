package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchasingDetailDTO {
    private String code;
    private String name;
    private Long qty;
    private BigDecimal price;
    private BigDecimal discAmount;
    private BigDecimal total;
    private BigDecimal markup1;
    private BigDecimal hargaJual1;
    private BigDecimal markup2;
    private BigDecimal hargaJual2;
    private BigDecimal markup3;
    private BigDecimal hargaJual3;

}
