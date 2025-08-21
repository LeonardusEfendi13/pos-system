package com.pos.posApps.DTO.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterFromDevRequest {

    private RegisterRequest registerRequest;

    @NotBlank(message = "Client Id cannot be empty")
    Long clientId;
}
