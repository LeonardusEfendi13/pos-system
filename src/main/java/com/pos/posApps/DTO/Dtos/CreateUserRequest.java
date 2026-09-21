package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String name;
    private String password;
    private String username;
    private String role;
}
