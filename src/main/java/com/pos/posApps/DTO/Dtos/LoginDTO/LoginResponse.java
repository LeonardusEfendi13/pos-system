package com.pos.posApps.DTO.Dtos.LoginDTO;

import lombok.Data;

@Data
public class LoginResponse {
    private String message;
    private String loginToken;
}
