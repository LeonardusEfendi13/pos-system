package com.pos.posApps.DTO.RegisterDTO;

import com.pos.posApps.DTO.EnumRole.Roles;
import lombok.Data;

@Data
public class RegisterRequest {
    String token;
    String name;
    String password;
    String username;
    Roles role;
    String salt;
}
