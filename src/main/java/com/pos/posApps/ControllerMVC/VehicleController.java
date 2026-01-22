package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.SidebarDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.VehicleEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.SidebarService;
import com.pos.posApps.Service.VehicleService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("vehicle")
@AllArgsConstructor
public class VehicleController {

    private AuthService authService;
    private VehicleService vehicleService;
    private SidebarService sidebarService;

    @GetMapping
    public String displayVehicle(HttpSession session, Model model){
        Long clientId;
        String token;
        try{
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            return "redirect:/login";
        }

        List<VehicleEntity> yamahaData = vehicleService.getVehicleYamahaList();
        List<VehicleEntity> hondaData = vehicleService.getVehicleHondaList();

        model.addAttribute("vehicleYamahaData", yamahaData);
        model.addAttribute("vehicleHondaData", hondaData);
        model.addAttribute("activePage", "vehicle");
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_vehicle";
    }

    @PostMapping("/add")
    public String addVehicle(String vehicleName, String vehicleBrand, String partNumber, HttpSession session, RedirectAttributes redirectAttributes){
        AccountEntity accEntity;
        ClientEntity clientData;
        System.out.println("Entering add");
        try{
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            clientData = accEntity.getClientEntity();
            if (clientData.getClientId() == null) {
                redirectAttributes.addFlashAttribute("status", true);
                redirectAttributes.addFlashAttribute("message", "Sesi anda habis, harap login ulang");
                return "redirect:/login";
            }
        }catch (Exception e){
            redirectAttributes.addFlashAttribute("message", "Sesi anda habis, harap login ulang");
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            ResponseInBoolean isInserted = vehicleService.insertVehicle(vehicleName, vehicleBrand, partNumber);
            redirectAttributes.addFlashAttribute("status", true);
            redirectAttributes.addFlashAttribute("message", isInserted.getMessage());
            return "redirect:/vehicle";
        }
        redirectAttributes.addFlashAttribute("status", true);
        redirectAttributes.addFlashAttribute("message", "Anda tidak punya akses!");
        return "redirect:/login";
    }

    @PostMapping("/edit")
    public String editVehicle(Long vehicleId, String vehicleName, String vehicleBrand, String partNumber, HttpSession session, RedirectAttributes redirectAttributes){
        AccountEntity accEntity;
        ClientEntity clientData;
        System.out.println("Entering edit");

        try{
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            clientData = accEntity.getClientEntity();
            if (clientData.getClientId() == null) {
                return "redirect:/login";
            }
        }catch (Exception e){
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isInserted = vehicleService.editVehicle(vehicleId, vehicleName, vehicleBrand, partNumber);
            if (isInserted) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Updated");
                return "redirect:/vehicle";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to update data");
            return "redirect:/vehicle";
        }
        return "redirect:/login";
    }

    @PostMapping("/delete/{vehicleId}")
    public String deleteVehicle(@PathVariable("vehicleId") Long vehicleId, HttpSession session, RedirectAttributes redirectAttributes){
        AccountEntity accEntity;
        ClientEntity clientData;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            clientData = accEntity.getClientEntity();
            if (clientData.getClientId() == null) {
                return "redirect:/login";
            }
        }catch (Exception e){
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isDeleted = vehicleService.deleteVehicle(vehicleId);
            if (isDeleted) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Deleted");
                return "redirect:/vehicle";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to delete data");
            return "redirect:/vehicle";
        }
        return "redirect:/login";
    }
}
