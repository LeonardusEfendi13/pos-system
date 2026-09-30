package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndenBootstrapDTO {
    private KasirClientDTO client;
    private IndenDTO inden;
    private String role;
    private List<PaymentMethodDTO> paymentMethods;
}
