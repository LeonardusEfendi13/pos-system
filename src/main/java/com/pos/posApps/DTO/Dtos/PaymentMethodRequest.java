package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class PaymentMethodRequest {
    private String name;
    private String methodType;
    private String rekening;
}
