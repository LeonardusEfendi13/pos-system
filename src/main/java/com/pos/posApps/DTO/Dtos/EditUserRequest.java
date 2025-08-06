package com.pos.posApps.DTO.Dtos;

import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import lombok.Data;

@Data
public class EditUserRequest {
    String id;
    String name;
    String password;
    String username;
    Roles role;
}
