package com.pos.posApps.Controller;

import com.pos.posApps.DTO.Dtos.RegisterDTO.RegisterRequest;
import com.pos.posApps.Service.AccountService;
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
@RequestMapping("api/v1/cashier")
@AllArgsConstructor
public class CashierController {

    private AccountService accountService;
    private AuthService authService;

    @PostMapping("/add-cashier")
    public ResponseEntity<String> register(
//            HttpServletRequest headerRequest,
            HttpSession session,
            @Valid @RequestBody RegisterRequest registerRequest){
//        String headerToken = headerRequest.getHeader("Authorization");
//        if (headerToken == null || !headerToken.startsWith("Bearer ")) {
//            return new ResponseEntity<>("Authorization header is missing or invalid", HttpStatus.BAD_REQUEST);
//        }
//        String token = headerToken.substring(7);
        String token = (String) session.getAttribute(authSessionKey);
        String clientId = authService.validateToken(token);
        if(clientId == null){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        boolean isInserted = accountService.doCreateAccount(registerRequest, clientId);
        if(isInserted){
            return new ResponseEntity<>("Account created", HttpStatus.CREATED);
        }
        return new ResponseEntity<>("Account not created", HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
