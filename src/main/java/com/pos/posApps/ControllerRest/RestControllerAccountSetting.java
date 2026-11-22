package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.UpdateFontWeightRequest;
import com.pos.posApps.Service.AccountSettingService;
import com.pos.posApps.Service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

// Self-service: every role may change its own preferences. The account always
// comes from the session, never from the request body.
@RestController
@RequestMapping("api/account/me")
@AllArgsConstructor
public class RestControllerAccountSetting {

    private AuthService authService;
    private AccountSettingService accountSettingService;

    @PutMapping("/font-weight")
    public ResponseEntity<ResponseInBoolean> updateFontWeight(
            @RequestBody UpdateFontWeightRequest req,
            HttpSession session
    ) {
        Long accountId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accountId = authService.validateToken(token).getAccountId();
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }

        if (!accountSettingService.updateFontWeightLevel(accountId, req.getLevel())) {
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new ResponseInBoolean(false, "Level ketebalan harus 1 sampai 4"));
        }

        return ResponseEntity.ok(new ResponseInBoolean(true, "Level ketebalan disimpan"));
    }
}
