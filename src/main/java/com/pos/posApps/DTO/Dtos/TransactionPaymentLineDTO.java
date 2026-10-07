package com.pos.posApps.DTO.Dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionPaymentLineDTO {
    private Long paymentMethodId;
    private BigDecimal amount;
}
