package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.ClientDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
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

    @GetMapping
    public String showSettings(HttpSession session, Model model) {
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        ClientDTO clientData = clientService.getClientSettings(clientId);
        //Convert DTO into Hash Map
        Map<String, String> clientSettings = new LinkedHashMap<>();
        clientSettings.put("NAMA", clientData.getName());
        clientSettings.put("ALAMAT", clientData.getAlamat());
        clientSettings.put("KOTA", clientData.getKota());
        clientSettings.put("NOMOR HP", clientData.getNoTelp());

        model.addAttribute("settingData", clientSettings);
        model.addAttribute("activePage", "setting");
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

            boolean isUpdated = clientService.updateClientField(clientId, fieldKey, fieldValue);
            if(!isUpdated){
                redirectAttributes.addFlashAttribute("status", "failed");
                redirectAttributes.addFlashAttribute("message", "Failed to update");
                return "redirect:/setting";
            }
            redirectAttributes.addFlashAttribute("status", "success");
            redirectAttributes.addFlashAttribute("message", "Success to update");
            return "redirect:/setting";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Session Expired");
            return "redirect:/login";
        }
    }
}
