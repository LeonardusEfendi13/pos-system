package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.SidebarDTO;
import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.CustomerService;
import com.pos.posApps.Service.SidebarService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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
@RequestMapping("customer")
@AllArgsConstructor
public class CustomerController {

    private CustomerService customerService;
    private AuthService authService;
    private SidebarService sidebarService;

    @GetMapping
    public String showCustomer(HttpSession session, Model model) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        List<CustomerEntity> customerList = customerService.getCustomerList(clientId);
        model.addAttribute("customerData", customerList);
        model.addAttribute("activePage", "customer");
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_customer";
    }

    @PostMapping("/add")
    public String registerCustomer(
            HttpSession session,
            @Valid String customerName,
            @Valid String customerAlamat,
            RedirectAttributes redirectAttributes) {
        ClientEntity clientData;
        AccountEntity accEntity;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            clientData = accEntity.getClientEntity();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Session Expired");
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isInserted = customerService.doCreateCustomer(customerName, customerAlamat, clientData);
            if (isInserted) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Customer Created");
                return "redirect:/customer";
            }
        }
        redirectAttributes.addFlashAttribute("status", "failed");
        redirectAttributes.addFlashAttribute("message", "Failed to Create Customer");
        return "redirect:/customer";
    }

    @PostMapping("/edit")
    public String editCustomer(
            HttpSession session,
            @Valid Long customerId,
            @Valid String customerName,
            @Valid String customerAlamat,
            RedirectAttributes redirectAttributes
    ) {
        Long clientId;
        AccountEntity accEntity;
        Roles role;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            role = accEntity.getRole();
            clientId = accEntity.getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(role)) {
            boolean isUpdated = customerService.doUpdateCustomer(customerId, customerName, clientId, customerAlamat);
            if (isUpdated) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Customer Updated");
                return "redirect:/customer";
            }

            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to update Customer");
            return "redirect:/customer";
        }
        redirectAttributes.addFlashAttribute("status", "failed");
        redirectAttributes.addFlashAttribute("message", "No Access to Edit Account");
        return "redirect:/login";
    }

    @PostMapping("/delete/{customerId}")
    public String deleteUser(
            HttpSession session,
            @PathVariable("customerId") Long customerId,
            RedirectAttributes redirectAttributes
    ) {
        Long clientId;
        AccountEntity accEntity;
        Roles role;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            role = accEntity.getRole();
            clientId = accEntity.getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(role)) {
            boolean isDeleted = customerService.deleteCustomer(customerId, clientId);
            if (isDeleted) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Customer Deleted");
                return "redirect:/customer";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to Delete Customer");
            return "redirect:/customer";
        }

        redirectAttributes.addFlashAttribute("status", "failed");
        redirectAttributes.addFlashAttribute("message", "No Access to Update Account Data");
        return "redirect:/login";
    }
}
