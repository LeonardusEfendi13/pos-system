package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PembelianHistoryRowDTO {
    private Long pembelianId;
    private String noFaktur;
    private LocalDateTime tanggalBeli;
    private LocalDateTime tanggalTempo;
    private BigDecimal totalPrice;
    private Long supplierId;
    private String supplierName;
    private boolean isCash;
    private boolean isPaid;
    private String accountName;
}
