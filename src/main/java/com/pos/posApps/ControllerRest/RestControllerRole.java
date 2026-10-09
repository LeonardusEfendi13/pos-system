package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientRoleService;
import com.pos.posApps.Service.EffectiveAccess;
import com.pos.posApps.Service.MenuAccessService;
import com.pos.posApps.Util.MenuCatalog;
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
import java.util.Optional;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/role")
@AllArgsConstructor
public class RestControllerRole {
    private AuthService authService;
    private MenuAccessService menuAccessService;
    private ClientRoleService clientRoleService;

    @GetMapping
    public ResponseEntity<List<ClientRoleService.ClientRoleView>> list(HttpSession session) {
        AccountEntity account = requireUserMenu(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(clientRoleService.list(account.getClientEntity().getClientId()));
    }

    @PostMapping
    public ResponseEntity<ResponseInBoolean> create(HttpSession session, @RequestBody RoleWriteRequest request) {
        return mutate(session, "Role berhasil ditambah", account -> clientRoleService.create(
                account.getClientEntity().getClientId(),
                request == null ? null : request.name()
        ));
    }

    @PutMapping("/{clientRoleId}")
    public ResponseEntity<ResponseInBoolean> update(
            HttpSession session,
            @PathVariable Long clientRoleId,
            @RequestBody RoleWriteRequest request
    ) {
        return mutate(session, "Role berhasil diubah", account -> clientRoleService.update(
                account.getClientEntity().getClientId(),
                clientRoleId,
                request == null ? null : request.name(),
                request == null ? List.of() : request.menuKeys()
        ));
    }

    @DeleteMapping("/{clientRoleId}")
    public ResponseEntity<ResponseInBoolean> delete(HttpSession session, @PathVariable Long clientRoleId) {
        return mutate(session, "Role berhasil dihapus", account -> clientRoleService.delete(
                account.getClientEntity().getClientId(),
                clientRoleId
        ));
    }

    private ResponseEntity<ResponseInBoolean> mutate(
            HttpSession session,
            String successMessage,
            java.util.function.Function<AccountEntity, Optional<String>> action
    ) {
        AccountEntity account = requireUserMenu(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }
        Optional<String> error = action.apply(account);
        if (error.isEmpty()) {
            return ResponseEntity.ok(new ResponseInBoolean(true, successMessage));
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(new ResponseInBoolean(false, error.get()));
    }

    private AccountEntity requireUserMenu(HttpSession session) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            if (account == null) {
                return null;
            }
            EffectiveAccess access = menuAccessService.resolve(account);
            if (access == null || !access.allows(MenuCatalog.USER)) {
                return null;
            }
            return account;
        } catch (Exception exception) {
            return null;
        }
    }

    public record RoleWriteRequest(String name, List<String> menuKeys) {
    }
}
