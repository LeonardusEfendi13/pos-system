package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class CreateVehicleRequest {
    private String model;
    private String brand;
    private String knownPartNumber;
}
