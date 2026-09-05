package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.KasirCustomerDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.CustomerService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/customer")
@AllArgsConstructor
public class RestControllerCustomer {
    private AuthService authService;
    private CustomerService customerService;

    @GetMapping("/list")
    public ResponseEntity<List<KasirCustomerDTO>> list(HttpSession session) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();
            List<KasirCustomerDTO> rows = customerService.getCustomerList(clientId)
                    .stream()
                    .map(c -> new KasirCustomerDTO(
                            c.getCustomerId(),
                            c.getName(),
                            c.getAlamat(),
                            c.isKing()))
                    .toList();
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body(Collections.emptyList());
        }
    }
}
