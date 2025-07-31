package com.pos.posApps.Controller;

import com.pos.posApps.DTO.Dtos.CreateClientDTO.CreateClientRequest;
import com.pos.posApps.DTO.Dtos.RegisterFromDevDTO.RegisterFromDevRequest;
import com.pos.posApps.Service.AccountService;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
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

//For Developer Only
@RestController
@RequestMapping("api/v1/god")
@AllArgsConstructor
public class DeveloperController {

    private AuthService authService;
    private ClientService clientService;
    private AccountService accountService;

    @PostMapping("/add-client")
    public ResponseEntity<String> createClient(
            @Valid @RequestBody CreateClientRequest req,
            HttpSession session
    ) {
        String token = (String) session.getAttribute(authSessionKey);
        String clientId = authService.validateToken(token);

        // Check if clientId is null or the clientId is not CLNO (Developer)
        if (clientId == null || !clientId.equalsIgnoreCase("CLN0")) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        boolean isInserted = clientService.doCreateClient(req);
        if(isInserted){
            return new ResponseEntity<>("New Client Created", HttpStatus.OK);
        }
        return new ResponseEntity<>("Failed to create client", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/add-cashier")
    public ResponseEntity<String> createAccount(
            @Valid @RequestBody RegisterFromDevRequest req, HttpSession session)
    {
        String token = (String) session.getAttribute(authSessionKey);
        String clientId = authService.validateToken(token);

        // Check if clientId is null or the clientId is not CLNO (Developer)
        if (clientId == null || !clientId.equalsIgnoreCase("CLN0")) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        boolean isInserted = accountService.doCreateAccount(req.getRegisterRequest(), req.getClientId());
        if(isInserted){
            return new ResponseEntity<>("Account created", HttpStatus.CREATED);
        }
        return new ResponseEntity<>("Account not created", HttpStatus.OK);
    }
}
