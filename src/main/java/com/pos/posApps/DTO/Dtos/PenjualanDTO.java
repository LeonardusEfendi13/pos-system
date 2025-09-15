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
public class PenjualanDTO {
    private Long transactionId;
    private CustomerDTO customerDTO;
    private String transactionNumber;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;
    private BigDecimal totalDisc;
    private LocalDateTime tanggalJual;
    private List<TransactionDetailDTO> transactionDetailDTOS;
}
