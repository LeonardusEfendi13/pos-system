package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

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
    private BigDecimal cashAmount;
    private BigDecimal transferAmount;
    private BigDecimal qrisAmount;
    private BigDecimal creditAmount;
    // Per type, then per method; the four amounts above stay for the PDFs
    private List<LaporanPaymentTypeDTO> payments;
}
