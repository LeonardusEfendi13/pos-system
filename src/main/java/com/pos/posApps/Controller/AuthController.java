package com.pos.posApps.Controller;

import com.pos.posApps.DTO.Dtos.LoginDTO.LoginRequest;
import com.pos.posApps.DTO.Dtos.LoginDTO.LoginResponse;
import com.pos.posApps.Service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@RestController
@RequestMapping("api/v1")
@AllArgsConstructor
public class AuthController {

    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpSession httpSession) {
        String token = authService.doLoginAndGetToken(loginRequest.getUsername(), loginRequest.getPassword());
        LoginResponse loginResponse  = new LoginResponse();
        if(token == null){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        httpSession.setAttribute(authSessionKey, token);
        loginResponse.setMessage("Successfully logged in");
        loginResponse.setLoginToken(token);
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }




}
