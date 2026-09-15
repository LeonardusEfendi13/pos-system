package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.SidebarDTO;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.SidebarService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/session")
@AllArgsConstructor
public class RestControllerSession {

    private AuthService authService;
    private SidebarService sidebarService;

    @GetMapping
    public ResponseEntity<SidebarDTO> getSession(HttpSession session) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            Long clientId = authService.validateToken(token).getClientEntity().getClientId();
            SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);

            return ResponseEntity.ok(sidebarData);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }
}
