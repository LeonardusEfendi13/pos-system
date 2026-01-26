package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompatibleProductsDTO {
    private Long productId;
    private Long vehicleId;
    private String yearStart;
    private String yearEnd;
    private String model;
}
