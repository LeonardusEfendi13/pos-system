package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordedPaymentDTO {
    private Long paymentMethodId;
    private String paymentMethodName;
    private String paymentMethodType;
    private BigDecimal amount;
    private Integer sortOrder;
}
