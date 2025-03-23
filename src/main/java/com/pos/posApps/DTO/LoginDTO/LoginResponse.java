package com.pos.posApps.DTO.LoginDTO;

import lombok.Data;

@Data
public class LoginResponse {
    private String message;
    private String loginToken;
}
