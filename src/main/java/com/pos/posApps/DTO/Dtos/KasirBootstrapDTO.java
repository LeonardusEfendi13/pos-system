package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KasirBootstrapDTO {
    private List<KasirCustomerDTO> customers;
    private KasirClientDTO client;
    private PenjualanDTO transaction;
    private String role;
    private List<PaymentMethodDTO> paymentMethods;
    // Active pretels of the edited sale, so the cart can tag its component rows
    private List<KasirPretelDTO> pretels = List.of();

    public KasirBootstrapDTO(
            List<KasirCustomerDTO> customers,
            KasirClientDTO client,
            PenjualanDTO transaction,
            String role,
            List<PaymentMethodDTO> paymentMethods
    ) {
        this.customers = customers;
        this.client = client;
        this.transaction = transaction;
        this.role = role;
        this.paymentMethods = paymentMethods;
    }
}
