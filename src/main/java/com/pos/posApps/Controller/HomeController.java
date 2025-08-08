package com.pos.posApps.Controller;

import com.pos.posApps.Service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("home")
@AllArgsConstructor
public class HomeController {

    private AuthService authService;

    public String home(HttpSession session){
        String token = (String) session.getAttribute(authSessionKey);
        String clientId = authService.validateToken(token).getClientEntity().getClientId();
        if(clientId == null){
            System.out.println("Failed");
            return "login";
        }
        System.out.println("Welcome to home");
        return "home";
    }
}
