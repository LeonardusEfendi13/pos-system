package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPengeluaranPeriodePageDTO {
    private List<LaporanPembelianPerWaktuDTO> content;
    private BigDecimal totalPembelian;
    private String startDate;
    private String endDate;
    private String filterOptions;
    private String role;
    private String accountName;
}
