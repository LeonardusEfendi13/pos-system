package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PembelianHistoryDetailDTO {
    private Long pembelianId;
    private String noFaktur;
    private LocalDateTime tanggalBeli;
    private LocalDateTime tanggalTempo;
    private BigDecimal totalPrice;
    private BigDecimal totalDisc;
    private BigDecimal subtotal;
    private String supplierName;
    private boolean isCash;
    private boolean isPaid;
    private String accountName;
    private List<PembelianHistoryLineDTO> lines;
}
