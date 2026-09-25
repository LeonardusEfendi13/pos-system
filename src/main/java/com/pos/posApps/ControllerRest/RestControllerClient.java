package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.ClientDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.UpdateClientSettingsRequest;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
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

    @PutMapping("/settings")
    public ResponseEntity<ResponseInBoolean> updateSettings(
            HttpSession session,
            @RequestBody UpdateClientSettingsRequest req
    ) {
        AccountEntity account;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            account = authService.validateToken(token);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }

        if (!authService.hasAccessToModifyData(account.getRole())) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Anda tidak memiliki akses untuk ini!"));
        }

        Long clientId = account.getClientEntity().getClientId();
        ResponseInBoolean result = clientService.updateClientSettings(clientId, req);

        if (!result.isStatus()) {
            return ResponseEntity.status(BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }
}
