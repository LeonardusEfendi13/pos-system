package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.IndenBootstrapDTO;
import com.pos.posApps.DTO.Dtos.IndenDTO;
import com.pos.posApps.DTO.Dtos.KasirClientDTO;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
import com.pos.posApps.Service.IndenService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/inden")
@AllArgsConstructor
public class RestControllerIndenBootstrap {
    private AuthService authService;
    private ClientService clientService;
    private IndenService indenService;

    @GetMapping("/bootstrap")
    public ResponseEntity<IndenBootstrapDTO> bootstrap(
            HttpSession session,
            @RequestParam(required = false) Long indenId) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            var account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();
            var settings = clientService.getClientSettings(clientId);
            KasirClientDTO client = settings == null
                    ? new KasirClientDTO("", "", "", "", "")
                    : new KasirClientDTO(
                            settings.getName(),
                            settings.getAlamat(),
                            settings.getKota(),
                            settings.getNoTelp(),
                            settings.getCatatan() == null ? "" : settings.getCatatan());
            IndenDTO inden = null;
            if (indenId != null) {
                inden = indenService.getPenjualanDataById(indenId);
            }
            return ResponseEntity.ok(
                    new IndenBootstrapDTO(
                            client,
                            inden,
                            account.getRole().name()));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }
}
