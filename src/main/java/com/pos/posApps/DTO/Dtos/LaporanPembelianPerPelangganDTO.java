package com.pos.posApps.DTO.Dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPembelianPerPelangganDTO {
    private String supplierName;
    private BigDecimal totalHargaPembelian;
}
