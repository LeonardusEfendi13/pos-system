package com.pos.posApps.Controller;

import com.pos.posApps.DTO.Dtos.CreatePreorderRequest;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.PreorderEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.PreorderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("preorder")
public class PreorderController {
    private AuthService authService;
    private PreorderService preorderService;

    @GetMapping("/list-preorder")
    public String showListPreorder(String supplierId, HttpSession session, Model model){
        String token = (String) session.getAttribute(authSessionKey);
        String clientId = authService.validateToken(token).getClientEntity().getClientId();
        if(clientId == null){
            System.out.println("Failed");
            return "login";
        }
        List<PreorderEntity> preorderEntity = preorderService.getPreorderData(clientId, supplierId);
        model.addAttribute("preorderEntity", preorderEntity);
        return "display_preorder";
    }

    @PostMapping("/add-preorder")
    public String addPreorder(HttpSession session, CreatePreorderRequest req){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "401";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isInserted = preorderService.insertPreorder(req, clientData);
            if (isInserted) {
                return "200";
            }
            return "500";
        }
        return "401";
    }
}
