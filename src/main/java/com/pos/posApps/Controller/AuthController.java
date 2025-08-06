package com.pos.posApps.Controller;

import com.pos.posApps.DTO.Dtos.LoginRequest;
import com.pos.posApps.Service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@AllArgsConstructor
public class AuthController {

    private AuthService authService;


    @GetMapping("/")
    public String redirect(HttpSession session) {
        String token = (String) session.getAttribute(authSessionKey);
        String clientId = authService.validateToken(token).getClientEntity().getClientId();
        if(clientId == null){
            System.out.println("Masuk sini");
            return "redirect:/login";
        }
        return "redirect:/api/v1/index";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model, HttpSession session){
        System.out.println("Masuk login page");
        model.addAttribute("loginRequest", new LoginRequest());
        if(session.getAttribute(authSessionKey) != null){
            return "home";
        }
        return "login";
    }

    @PostMapping("/doLogin")
    public String login(
            @Valid LoginRequest loginRequest,
            HttpSession httpSession,
            BindingResult bindingResult,
            Model model) {
        System.out.println("Masuk Login");
        String token = authService.doLoginAndGetToken(loginRequest.getUsername(), loginRequest.getPassword());
        if(bindingResult.hasErrors()){
            model.addAttribute("login_status", 200);
            return "redirect:/login";
        }
        if(token == null){
            model.addAttribute("login_status", 401);
            return "redirec:/login";
        }
        httpSession.setAttribute(authSessionKey, token);
        return "redirect:/api/v1/home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, Model model){
        session.invalidate();
        model.addAttribute("logout_status", 200);
        return "login";
    }
}
