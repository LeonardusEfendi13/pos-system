package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPendapatanPeriodePageDTO {
    private List<LaporanPenjualanPerWaktuDTO> content;
    private BigDecimal totalKotor;
    private BigDecimal totalBersih;
    private List<CustomerLookupDTO> customers;
    private String startDate;
    private String endDate;
    private Long customerId;
    private String filterOptions;
    private String role;
    private String accountName;
}
