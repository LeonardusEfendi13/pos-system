package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.SidebarDTO;
import com.pos.posApps.Entity.DataCenterLogEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.DataCenterService;
import com.pos.posApps.Service.SidebarService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("data_center")
@AllArgsConstructor
public class DataCenterController {

    private AuthService authService;
    private SidebarService sidebarService;
    private DataCenterService dataCenterService;

    @GetMapping
    public String displayBackupPage(HttpSession session, Model model){
        Long clientId;
        String token;
        try{
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            return "redirect:/login";
        }

        List<DataCenterLogEntity> data = dataCenterService.getLogData();
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("log", data);
        model.addAttribute("activePage", "dataCenter");
        model.addAttribute("sidebarData", sidebarData);
        return "display_data_center";
    }
}
