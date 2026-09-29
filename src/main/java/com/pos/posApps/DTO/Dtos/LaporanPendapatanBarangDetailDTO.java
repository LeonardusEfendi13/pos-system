package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPendapatanBarangDetailDTO {
    private Long productId;
    private String productName;
    private String supplierName;
    private List<LaporanPenjualanBarangBucketDTO> content;
    private Long totalQty;
    private BigDecimal totalKotor;
    private BigDecimal totalBersih;
    private String startDate;
    private String endDate;
    private String filterOptions;
    private String role;
    private String accountName;
}
