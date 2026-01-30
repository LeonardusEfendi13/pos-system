package com.pos.posApps.DTO.Dtos;

import com.pos.posApps.Entity.VehicleEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchContextDTO {
    private List<VehicleEntity> vehicles;
    private String rawKeyword;
    private String productKeyword;
}
