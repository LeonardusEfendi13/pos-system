package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KasirCustomerDTO {
    private Long customerId;
    private String name;
    private String alamat;
    private boolean king;
}
