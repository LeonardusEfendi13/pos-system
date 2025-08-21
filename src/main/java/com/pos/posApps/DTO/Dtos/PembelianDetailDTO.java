package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PembelianDetailDTO {
    private Long pembelianDetailId;
    private String code;
    private String name;
    private Long qty;
    private BigDecimal price;
    private BigDecimal discAmount;
    private BigDecimal total;
}
