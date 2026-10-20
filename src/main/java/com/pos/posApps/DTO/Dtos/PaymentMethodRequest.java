package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class PaymentMethodRequest {
    private String name;
    private Long paymentTypeId;
    // Legacy: used only when paymentTypeId is missing
    private String methodType;
    private String rekening;
}
