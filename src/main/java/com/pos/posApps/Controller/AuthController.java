package com.pos.posApps.Controller;

import com.pos.posApps.DTO.LoginDTO.LoginRequest;
import com.pos.posApps.DTO.LoginDTO.LoginResponse;
import com.pos.posApps.DTO.RegisterDTO.RegisterRequest;
import com.pos.posApps.Service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1")
@AllArgsConstructor
public class AuthController {

    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String token = authService.doLoginAndGetToken(loginRequest.getUsername(), loginRequest.getPassword());
        LoginResponse loginResponse  = new LoginResponse();
        if(token == null){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        loginResponse.setMessage("Successfully logged in");
        loginResponse.setLoginToken(token);
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }

    @PostMapping("/add-admin")
    public ResponseEntity<String> register(HttpServletRequest headerRequest, @Valid @RequestBody RegisterRequest registerRequest){
        String headerToken = headerRequest.getHeader("Authorization");
        if (headerToken == null || !headerToken.startsWith("Bearer ")) {
            return new ResponseEntity<>("Authorization header is missing or invalid", HttpStatus.BAD_REQUEST);
        }
        String token = headerToken.substring(7);
        String clientId = authService.validateToken(token);
        if(clientId == null){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        boolean isInserted = authService.doCreateAccount(registerRequest, clientId);
        if(isInserted){
            return new ResponseEntity<>("Account created", HttpStatus.CREATED);
        }
        return new ResponseEntity<>("Account not created", HttpStatus.OK);
    }

}
