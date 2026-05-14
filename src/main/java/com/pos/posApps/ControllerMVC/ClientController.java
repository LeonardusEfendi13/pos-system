package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.ClientDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.SidebarDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
import com.pos.posApps.Service.SidebarService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("setting")
@AllArgsConstructor
public class ClientController {

    private AuthService authService;
    private ClientService clientService;
    private SidebarService sidebarService;

    @GetMapping
    public String showSettings(HttpSession session, Model model) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        ClientDTO clientData = clientService.getClientSettings(clientId);
        //Convert DTO into Hash Map
        String kingDisc = "0";
        if(clientData.getKingDisc() != null){
            kingDisc = clientData.getKingDisc().toString();
        }
        Map<String, String> clientSettings = new LinkedHashMap<>();
        clientSettings.put("NAMA", clientData.getName());
        clientSettings.put("ALAMAT", clientData.getAlamat());
        clientSettings.put("KOTA", clientData.getKota());
        clientSettings.put("NOMOR HP", clientData.getNoTelp());
        clientSettings.put("CATATAN", clientData.getCatatan());
        clientSettings.put("KING DISC", kingDisc + "%");

        model.addAttribute("settingData", clientSettings);
        model.addAttribute("activePage", "setting");
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_setting";
    }

    @PostMapping("/update")
    public String updateClientField(
            @RequestParam String fieldKey,
            @RequestParam String fieldValue,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long clientId;
        AccountEntity accEntity;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            clientId = accEntity.getClientEntity().getClientId();

            ResponseInBoolean isUpdated = clientService.updateClientField(clientId, fieldKey, fieldValue);
            System.out.println("Status : "+ isUpdated);
            redirectAttributes.addFlashAttribute("status", isUpdated.isStatus());
            redirectAttributes.addFlashAttribute("message", isUpdated.getMessage());
            return "redirect:/setting";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Session Expired");
            System.out.println("depan : " + e.getMessage());

            return "redirect:/login";
        }
    }
}
