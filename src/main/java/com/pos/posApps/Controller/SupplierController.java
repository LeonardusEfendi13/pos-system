package com.pos.posApps.Controller;

import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.SupplierService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("api/v1/supplier")
@AllArgsConstructor
public class SupplierController {

    private AuthService authService;
    private SupplierService supplierService;

    @GetMapping("/list-supplier")
    public String displaySupplier(HttpSession session, Model model){
        String token = (String) session.getAttribute(authSessionKey);
        String clientId = authService.validateToken(token).getClientEntity().getClientId();
        if(clientId == null){
            System.out.println("Failed");
            return "login";
        }
        List<SupplierEntity> supplierEntityList = supplierService.getSupplierList(clientId);
        model.addAttribute("supplierData", supplierEntityList);
        return "display_supplier";
    }

    @PostMapping("/add-supplier")
    public String addSupplier(String supplierName, HttpSession session){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "401";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isInserted = supplierService.insertSupplier(supplierName, clientData);
            if (isInserted) {
                return "200";
            }
            return "500";
        }
        return "401";
    }

    @PostMapping("/edit-supplier")
    public String editSupplier(String supplierId, String supplierName, HttpSession session){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "401";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = supplierService.editSupplier(supplierId, supplierName, clientData);
            if(isEdited){
                return "200";
            }
            return "500";
        }
        return "401";
    }

    @PostMapping("/disable-supplier")
    public String disableSupplier(String supplierId, HttpSession session){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "401";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = supplierService.disableSupplier(supplierId, clientData);
            if(isEdited){
                return "200";
            }
            return "500";
        }
        return "401";
    }
}
