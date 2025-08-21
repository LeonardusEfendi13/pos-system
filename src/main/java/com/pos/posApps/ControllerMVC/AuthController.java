package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.LoginRequest;
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

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@AllArgsConstructor
public class AuthController {

    private AuthService authService;

    private LoginTokenService loginTokenService;


    @GetMapping("/")
    public String redirect(HttpSession session) {
        return Utils.validateSession(session, null, "home");
//        String token = (String) session.getAttribute(authSessionKey);
//        AccountEntity accountData = authService.validateToken(token);
//        if(accountData == null){
//            return "redirect:/login";
//        }
//        ClientEntity clientData = accountData.getClientEntity();
//        if(clientData.getClientId() == null){
//            System.out.println("Masuk sini");
//            return "redirect:/login";
//        }
//        return "redirect:/home";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model, HttpSession session){
        System.out.println("Masuk login page");
        model.addAttribute("loginRequest", new LoginRequest());
        if(session.getAttribute(authSessionKey) != null){
            return "redirect:/home";
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
            Model model) {
        System.out.println("Masuk Login");
        if(bindingResult.hasErrors()){
            model.addAttribute("login_status", "200");
            return "redirect:/login";
        }
        String token = authService.doLoginAndGetToken(loginRequest.getUsername(), loginRequest.getPassword());

        if(token == null){
            model.addAttribute("login_status", "401");
            return "redirect:/login";
        }
        httpSession.setAttribute(authSessionKey, token);
        model.addAttribute("activePage", "dashboard");
        return "redirect:/home";
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
