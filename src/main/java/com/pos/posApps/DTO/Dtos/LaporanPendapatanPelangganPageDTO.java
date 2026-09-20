package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPendapatanPelangganPageDTO {
    private List<LaporanPenjualanPerPelangganDTO> content;
    private BigDecimal totalKotor;
    private BigDecimal totalBersih;
    private String startDate;
    private String endDate;
    private String role;
    private String accountName;
}
