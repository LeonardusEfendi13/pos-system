package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentTypeDTO {
    private Long paymentTypeId;
    private String name;
    private String behavior;
    private boolean systemDefault;
}
