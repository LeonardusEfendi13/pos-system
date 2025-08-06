package com.pos.posApps.DTO.Dtos.UserDTO;


import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private String name;
    private String username;
    private Roles role;
}
