package com.pos.posApps.DTO.Dtos;

import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import lombok.Data;

@Data
public class EditUserRequest {
    Long id;
    String name;
    String username;
    Roles role;
}
