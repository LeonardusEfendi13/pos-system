package com.pos.posApps.DTO.Dtos.RegisterFromDevDTO;

import com.pos.posApps.DTO.Dtos.RegisterDTO.RegisterRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterFromDevRequest {

    private RegisterRequest registerRequest;

    @NotBlank(message = "Client Id cannot be empty")
    String clientId;
}
