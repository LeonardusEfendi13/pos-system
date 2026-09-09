package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.ClientDTO;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/client")
@AllArgsConstructor
public class RestControllerClient {

    private AuthService authService;
    private ClientService clientService;

    @GetMapping("/settings")
    public ResponseEntity<ClientDTO> getSettings(HttpSession session) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            Long clientId = authService.validateToken(token).getClientEntity().getClientId();
            ClientDTO settings = clientService.getClientSettings(clientId);

            if (settings == null) {
                return ResponseEntity.status(NOT_FOUND).build();
            }

            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }
}
