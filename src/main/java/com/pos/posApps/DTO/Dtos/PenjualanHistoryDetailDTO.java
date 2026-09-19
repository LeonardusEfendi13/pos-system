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
public class PenjualanHistoryDetailDTO {
    private Long transactionId;
    private String transactionNumber;
    private LocalDateTime tanggalJual;
    private BigDecimal totalPrice;
    private String customerName;
    private String accountName;
    private List<PenjualanHistoryLineDTO> lines;
}
