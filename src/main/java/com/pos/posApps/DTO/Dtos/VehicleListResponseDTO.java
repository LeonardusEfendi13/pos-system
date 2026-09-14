package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleListResponseDTO {
    private List<VehicleDTO> yamaha;
    private List<VehicleDTO> honda;
    private String role;
    private String accountName;
}
