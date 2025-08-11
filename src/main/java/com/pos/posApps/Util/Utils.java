package com.pos.posApps.Util;

import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;

import static com.pos.posApps.Constants.Constant.authSessionKey;

public class Utils {

    @Autowired
    private static AuthService authService;

    public static String validateSession(HttpSession session, String targetEndpoint, String targetPage ){
        try{
            String token = (String) session.getAttribute(authSessionKey);
            String clientId = authService.validateToken(token).getClientEntity().getClientId();
            if(clientId == null){
                System.out.println("Masuk sini");
                return "redirect:/login";
            }
            if(targetEndpoint != null && targetPage == null){
                return "redirect:/" + targetEndpoint;
            }
            return "redirect:/" + targetPage;
        }catch (Exception e){
            return "redirect:/login";
        }

    }
}
