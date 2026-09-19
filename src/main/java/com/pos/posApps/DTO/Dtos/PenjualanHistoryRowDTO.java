package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PenjualanHistoryRowDTO {
    private Long transactionId;
    private String transactionNumber;
    private LocalDateTime tanggalJual;
    private BigDecimal totalPrice;
    private Long customerId;
    private String customerName;
    private String accountName;
}
