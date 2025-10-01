package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.SidebarDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.SidebarService;
import com.pos.posApps.Service.SupplierService;
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
@RequestMapping("supplier")
@AllArgsConstructor
public class SupplierController {

    private AuthService authService;
    private SupplierService supplierService;
    private SidebarService sidebarService;

    @GetMapping
    public String displaySupplier(HttpSession session, Model model){
        Long clientId;
        String token;
        try{
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            return "redirect:/login";
        }

        List<SupplierEntity> supplierEntityList = supplierService.getSupplierList(clientId);
        model.addAttribute("supplierData", supplierEntityList);
        model.addAttribute("activePage", "supplier");
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_supplier";
    }

    @PostMapping("/add")
    public String addSupplier(String supplierName, HttpSession session, RedirectAttributes redirectAttributes){
        AccountEntity accEntity;
        ClientEntity clientData;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            clientData = accEntity.getClientEntity();
            if (clientData.getClientId() == null) {
                System.out.println("No Access to products");
                return "redirect:/login";
            }
        }catch (Exception e){
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isInserted = supplierService.insertSupplier(supplierName, clientData);
            if (isInserted) {
                System.out.println("Success cuy");
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data created");
                return "redirect:/supplier";
            }
            System.out.println("failed to create supplier");
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to create data");
            return "redirect:/supplier";
        }
        return "redirect:/login";
    }

    @PostMapping("/edit")
    public String editSupplier(Long supplierId, String supplierName, HttpSession session, RedirectAttributes redirectAttributes){
        AccountEntity accEntity;
        ClientEntity clientData;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            clientData = accEntity.getClientEntity();
            if (clientData.getClientId() == null) {
                System.out.println("No Access to products");
                return "redirect:/login";
            }
        }catch (Exception e){
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = supplierService.editSupplier(supplierId, supplierName, clientData);
            if(isEdited){
                System.out.println("success edit");
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Edited");
                return "redirect:/supplier";
            }
            System.out.println("Failed edit");
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to edit Data");
            return "redirect:/supplier";
        }
        return "redirect:/login";
    }

    @PostMapping("/delete/{supplierId}")
    public String disableSupplier(
            @PathVariable("supplierId") Long supplierId, HttpSession session, RedirectAttributes redirectAttributes){
        AccountEntity accEntity;
        ClientEntity clientData;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            clientData = accEntity.getClientEntity();
            if (clientData.getClientId() == null) {
                System.out.println("No Access to products");
                return "redirect:/login";
            }
        }catch (Exception e){
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = supplierService.disableSupplier(supplierId, clientData);
            if(isEdited){
                System.out.println("success delete");

                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Deleted");
                return "redirect:/supplier";
            }
            System.out.println("Failed to delete");
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to delete data");
            return "redirect:/supplier";
        }
        return "redirect:/login";
    }
}
