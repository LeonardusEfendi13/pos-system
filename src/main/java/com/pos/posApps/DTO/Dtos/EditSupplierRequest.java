package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class EditSupplierRequest {
    private Long supplierId;
    private String supplierName;
}
