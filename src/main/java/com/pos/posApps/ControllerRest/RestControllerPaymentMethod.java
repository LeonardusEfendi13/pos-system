package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.PaymentMethodDTO;
import com.pos.posApps.DTO.Dtos.PaymentMethodRequest;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.PaymentMethodService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/client/payment-methods")
@AllArgsConstructor
public class RestControllerPaymentMethod {
    private AuthService authService;
    private PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<PaymentMethodDTO>> list(HttpSession session) {
        try {
            Long clientId = account(session).getClientEntity().getClientId();
            return ResponseEntity.ok(paymentMethodService.list(clientId));
        } catch (Exception exception) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @PostMapping
    public ResponseEntity<ResponseInBoolean> create(
            HttpSession session,
            @RequestBody PaymentMethodRequest request
    ) {
        AccountEntity account = authorized(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }

        ResponseInBoolean result = paymentMethodService.create(
                account.getClientEntity(),
                request
        );
        if (!result.isStatus()) {
            return ResponseEntity.status(BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{paymentMethodId}")
    public ResponseEntity<ResponseInBoolean> update(
            HttpSession session,
            @PathVariable Long paymentMethodId,
            @RequestBody PaymentMethodRequest request
    ) {
        AccountEntity account = authorized(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }

        ResponseInBoolean result = paymentMethodService.update(
                account.getClientEntity().getClientId(),
                paymentMethodId,
                request
        );
        if (!result.isStatus()) {
            return ResponseEntity.status(BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<ResponseInBoolean> delete(
            HttpSession session,
            @PathVariable Long paymentMethodId
    ) {
        AccountEntity account = authorized(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }

        ResponseInBoolean result = paymentMethodService.delete(
                account.getClientEntity().getClientId(),
                paymentMethodId
        );
        if (!result.isStatus()) {
            return ResponseEntity.status(BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }

    private AccountEntity account(HttpSession session) throws Exception {
        String token = (String) session.getAttribute(authSessionKey);
        return authService.validateToken(token);
    }

    private AccountEntity authorized(HttpSession session) {
        try {
            AccountEntity account = account(session);
            if (!authService.hasAccessToModifyData(account.getRole())) {
                return null;
            }

            return account;
        } catch (Exception exception) {
            return null;
        }
    }
}
