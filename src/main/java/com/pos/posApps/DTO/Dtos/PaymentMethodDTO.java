package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodDTO {
    private Long paymentMethodId;
    private String name;
    private String methodType;
    private String rekening;
    private boolean systemDefault;
}
