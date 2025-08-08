package com.pos.posApps.Controller;

import com.pos.posApps.DTO.Dtos.EditUserRequest;
import com.pos.posApps.DTO.Dtos.RegisterRequest;
import com.pos.posApps.DTO.Dtos.UserDTO;
import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AccountService;
import com.pos.posApps.Service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("user")
@AllArgsConstructor
public class AccountController {

    private AccountService accountService;
    private AuthService authService;

    @GetMapping("/list-user")
    public String showUser(HttpSession session) {
        String token = (String) session.getAttribute(authSessionKey);
        String clientId = authService.validateToken(token).getClientEntity().getClientId();
        if (clientId == null) {
            return "logout";
        }

        List<UserDTO> userList = accountService.getUserList(clientId);
        return "display_user";
    }

    @PostMapping("/add-user")
    public String register(
            HttpSession session,
            @Valid RegisterRequest registerRequest) {
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        String clientId = accEntity.getClientEntity().getClientId();
        if (clientId == null) {
            System.out.println("No Access to register");
            return "401";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isInserted = accountService.doCreateAccount(registerRequest, clientId);
            if (isInserted) {
                System.out.println("Account Created");
                return "200";
            }
        }
        System.out.println("Account not created");
        return "500";
    }

    @PostMapping("/edit-user")
    public String editUser(
            HttpSession session,
            @Valid EditUserRequest request
    ) {
        String token = (String) session.getAttribute(authSessionKey);
        Roles role = authService.validateToken(token).getRole();
        if (authService.hasAccessToModifyData(role)) {
            boolean isUpdated = accountService.doUpdateAccount(request);
            if (isUpdated) {
                System.out.println("Account Updated");
                return "200";
            }
            System.out.println("Account Failed to update");
            return "500";
        }
        System.out.println("No Access to Update Account Data");
        return "401";
    }


}
