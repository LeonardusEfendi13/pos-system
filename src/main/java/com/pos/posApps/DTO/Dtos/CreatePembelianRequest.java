package com.pos.posApps.DTO.Dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePembelianRequest {
    private Long customerId;
    private List<TransactionDetailDTO> transactionDetailDTOS;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;
    private BigDecimal totalDisc;
}
