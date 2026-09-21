package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class CreateCustomerRequest {
    private String customerName;
    private String customerAlamat;
    private Boolean isKing;
}
