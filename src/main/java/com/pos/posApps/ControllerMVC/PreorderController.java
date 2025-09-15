package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.CreatePreorderRequest;
import com.pos.posApps.DTO.Dtos.EditPreorderRequest;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.PreorderEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.PreorderService;
import com.pos.posApps.Service.SupplierService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("preorder")
@AllArgsConstructor
public class PreorderController {
    private AuthService authService;
    private PreorderService preorderService;
    private SupplierService supplierService;

    @GetMapping
    public String showListPreorder(Long supplierId, String startDate, String endDate, HttpSession session, Model model){
        Long clientId;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            System.out.println("catch preorder");
            return "redirect:/login";
        }
        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank())? LocalDate.now().toString() : endDate;

        LocalDateTime inputStartDate = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime inputEndDate = LocalDate.parse(endDate).atTime(23, 59, 59);

        List<PreorderEntity> preorderEntity = preorderService.getPreorderData(clientId, supplierId, inputStartDate, inputEndDate);
        List<SupplierEntity> supplierData = supplierService.getSupplierList(clientId);
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("supplierData", supplierData);
        model.addAttribute("preorderData", preorderEntity);
        model.addAttribute("activePage", "preorder");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "display_preorder";
    }

    @PostMapping("/tambah")
    public String addPreorder(HttpSession session, CreatePreorderRequest req){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isInserted = preorderService.insertPreorder(req, clientData);
            if (isInserted) {
                return "200";
            }
            return "500";
        }
        return "redirect:/login";
    }

    @PostMapping("/edit-preorder")
    public String editPreorder(HttpSession session, EditPreorderRequest req){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isInserted = preorderService.editPreorder(req, clientData);
            if (isInserted) {
                return "200";
            }
            return "500";
        }
        return "redirect:/login";
    }

    @PostMapping("/delete-preorder")
    public String deletePreorder(HttpSession session, Long preorderId){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = preorderService.deleteProducts(preorderId);
            if(isEdited){
                return "200";
            }
            return "500";
        }
        return "redirect:/login";
    }
}
