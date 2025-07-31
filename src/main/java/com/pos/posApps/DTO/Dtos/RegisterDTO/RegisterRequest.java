package com.pos.posApps.DTO.Dtos.RegisterDTO;

import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Name cannot be empty")
    String name;

    @NotBlank(message = "Password cannot be empty")
    String password;

    @NotBlank(message = "Username cannot be empty")
    String username;

    @NotNull(message = "Role cannot be empty")
    Roles role;
}
