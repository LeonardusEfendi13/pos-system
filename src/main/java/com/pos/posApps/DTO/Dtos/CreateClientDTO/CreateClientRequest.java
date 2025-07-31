package com.pos.posApps.DTO.Dtos.CreateClientDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateClientRequest {
    @NotBlank(message = "Client Name cannot be empty")
    private String name;
}
