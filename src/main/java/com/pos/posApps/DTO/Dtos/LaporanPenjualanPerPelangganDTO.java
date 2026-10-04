package com.pos.posApps.DTO.Dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
}
