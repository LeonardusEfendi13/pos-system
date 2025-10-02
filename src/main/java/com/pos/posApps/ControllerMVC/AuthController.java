package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.LoginRequest;
import com.pos.posApps.DTO.Dtos.UserDTO;
import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Service.AccountService;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.LoginTokenService;
import com.pos.posApps.Util.Utils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@AllArgsConstructor
public class AuthController {

    private AuthService authService;

    private LoginTokenService loginTokenService;

    private AccountService accountService;


    @GetMapping("/")
    public String redirect(HttpSession session) {
        return Utils.validateSession(session, null, "home");
    }

    @GetMapping("/login")
    public String showLoginPage(Model model, HttpSession session){
        model.addAttribute("loginRequest", new LoginRequest());
        String token = (String) session.getAttribute(authSessionKey);
        if(token != null){
            UserDTO userData = accountService.getCurrentLoggedInUser(token);
            if(userData.getRole().equals(Roles.SUPER_ADMIN)){
                return "redirect:/home";
            }else{
                return "redirect:/kasir";
            }
        }
        return "login";
    }
    @GetMapping("/doLogin")
    public String handleGetDoLogin(){
        return "redirect:/login";
    }

    @PostMapping("/doLogin")
    public String login(
            @Valid LoginRequest loginRequest,
            HttpSession httpSession,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if(bindingResult.hasErrors()){
            redirectAttributes.addFlashAttribute("status", true);
            redirectAttributes.addFlashAttribute("message", "Username / Password salah");
            return "redirect:/login";
        }
        String token = authService.doLoginAndGetToken(loginRequest.getUsername(), loginRequest.getPassword());

        if(token == null){
            redirectAttributes.addFlashAttribute("status", true);
            redirectAttributes.addFlashAttribute("message", "Username / Password salah");
            return "redirect:/login";
        }
        httpSession.setAttribute(authSessionKey, token);
        model.addAttribute("activePage", "dashboard");
        UserDTO userData = accountService.getCurrentLoggedInUser(token);
        if(userData.getRole().equals(Roles.SUPER_ADMIN)){
            return "redirect:/home";
        }else{
            return "redirect:/kasir";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, Model model){
        //delete token
        boolean isDeleted = loginTokenService.deleteToken((String) session.getAttribute(authSessionKey));
        if(!isDeleted){
            return null;
        }
        session.invalidate();
        model.addAttribute("login_status", "logout");
        return "redirect:/login";
    }
}
