package com.pos.posApps.DTO.Dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPembelianPerWaktuDTO {
    private String period;
    private BigDecimal totalHargaPembelian;
}
