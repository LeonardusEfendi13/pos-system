package com.pos.posApps.Controller;

import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Util.Utils;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("home")
@AllArgsConstructor
public class HomeController {

    private AuthService authService;

    @GetMapping
    public String home(HttpSession session, Model model){
//        Utils.validateSession(session, null, "home");
        String clientId;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            return "redirect:/login";
        }

        System.out.println("Welcome to home");
        model.addAttribute("activePage", "home");
        return "home";
    }
}
