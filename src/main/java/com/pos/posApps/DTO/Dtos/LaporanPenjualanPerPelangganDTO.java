package com.pos.posApps.DTO.Dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPenjualanPerPelangganDTO {
    private Long customerId;
    private String customerName;
    private BigDecimal totalHargaPenjualan;
    private BigDecimal labaPenjualan;
    private Long transactionCount;
    private Long unpaidInvoiceCount;
    private BigDecimal unpaidTotal;
    private BigDecimal cashAmount;
    private BigDecimal transferAmount;
    private BigDecimal qrisAmount;
    private BigDecimal creditAmount;
    // Per type, then per method; the four amounts above stay for the PDFs
    private List<LaporanPaymentTypeDTO> payments;
}
