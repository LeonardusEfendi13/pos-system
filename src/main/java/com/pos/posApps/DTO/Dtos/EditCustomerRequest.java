package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class EditCustomerRequest {
    private Long customerId;
    private String customerName;
    private String customerAlamat;
    private Boolean isKing;
}
