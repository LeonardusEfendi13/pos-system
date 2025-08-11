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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("user")
@AllArgsConstructor
public class AccountController {

    private AccountService accountService;
    private AuthService authService;

    @GetMapping
    public String showUser(HttpSession session, Model model) {
        String clientId;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            return "redirect:/login";
        }
//        String token = (String) session.getAttribute(authSessionKey);
//        String clientId = authService.validateToken(token).getClientEntity().getClientId();
//        if (clientId == null) {
//            return "redirect:/login";
//        }

        List<UserDTO> userList = accountService.getUserList(clientId);
        System.out.println(userList);
        model.addAttribute("userData", userList);
        model.addAttribute("activePage", "user");
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
            return "redirect:/login";
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

    @PostMapping("/edit")
    public String editUser(
            HttpSession session,
            @Valid EditUserRequest request,
            RedirectAttributes redirectAttributes
    ) {
        Roles role;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            role = authService.validateToken(token).getRole();
        }catch (Exception e){
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(role)) {
            boolean isUpdated = accountService.doUpdateAccount(request);
            if (isUpdated) {
                redirectAttributes.addFlashAttribute("user_status", "success");
                redirectAttributes.addFlashAttribute("message", "Account Updated");
                return "redirect:/user";
            }
            redirectAttributes.addFlashAttribute("user_status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to update Account");
            return "redirect:/user";
        }
        //todo add redirect attributes
        System.out.println("No Access to Update Account Data");
        return "redirect:/login";
    }

    @PostMapping("/delete/{userId}")
    public String deleteUser(
            HttpSession session,
            @PathVariable("userId") String userId,
            RedirectAttributes redirectAttributes
    ) {
        Roles role;

        try{
            String token = (String) session.getAttribute(authSessionKey);
            role = authService.validateToken(token).getRole();
        }catch (Exception e){
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(role)) {
            boolean isDeleted = accountService.doDisableAccount(userId);
            if (isDeleted) {
                System.out.println("success delete");
                redirectAttributes.addFlashAttribute("user_status", "success");
                redirectAttributes.addFlashAttribute("message", "Account Created");
                return "redirect:/user";
            }
            System.out.println("failed delete");
            redirectAttributes.addFlashAttribute("user_status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to Delete Account");
            return "redirect:/user";
        }

        //todo add redirect attributes
        System.out.println("No Access to Update Account Data");
        return "redirect:/login";
    }


    @GetMapping("/detail/{userId}")
    public String getUserDetail(
            HttpSession session,
            @PathVariable("userId") String userId
    ){
        //todo
        return "redirect:/login";

    }


}
