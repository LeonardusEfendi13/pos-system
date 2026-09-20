package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPengeluaranPelangganPageDTO {
    private List<LaporanPembelianPerPelangganDTO> content;
    private BigDecimal totalPembelian;
    private String startDate;
    private String endDate;
    private String role;
    private String accountName;
}
