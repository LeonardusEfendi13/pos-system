package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPenjualanPerBarangDTO {
    private Long productId;
    private String productName;
    private String shortName;
    private String supplierName;
    private Long qty;
    private BigDecimal totalHargaPenjualan;
    private BigDecimal labaPenjualan;
}
