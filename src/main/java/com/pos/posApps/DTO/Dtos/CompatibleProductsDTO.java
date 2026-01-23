package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompatibleProductsDTO {
    private Long vehicleId;
    private String yearStart;
    private String yearEnd;
}
