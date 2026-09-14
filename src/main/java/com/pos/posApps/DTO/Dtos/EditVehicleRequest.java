package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class EditVehicleRequest {
    private Long vehicleId;
    private String model;
    private String brand;
    private String knownPartNumber;
}
