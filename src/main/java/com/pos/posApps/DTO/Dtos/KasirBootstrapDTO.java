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
}
