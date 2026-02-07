package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.*;
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
@RequestMapping("branch")
@AllArgsConstructor
public class BranchController {

    private AccountService accountService;
    private AuthService authService;
    private SidebarService sidebarService;
    private BranchService branchService;
    private CustomerService customerService;

    @GetMapping
    public String showBranches(HttpSession session, Model model) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        List<BranchDTO> branchList = branchService.getBranchList();
        List<CustomerEntity> customerList = customerService.getCustomerList(clientId);
        model.addAttribute("customerData", customerList);
        model.addAttribute("branchData", branchList);
        model.addAttribute("activePage", "branch");
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_branch";
    }

    @PostMapping("/add")
    public String register(
            HttpSession session,
            Long customerId,
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
            ResponseInBoolean isInserted = branchService.doCreateBranch(customerId, clientData);
            redirectAttributes.addFlashAttribute("status", isInserted.isStatus());
            redirectAttributes.addFlashAttribute("message", isInserted.getMessage());
            return "redirect:/branch";
        }
        return "redirect:/login";
    }

    @PostMapping("/delete/{branchId}")
    public String deleteUser(
            HttpSession session,
            @PathVariable("branchId") Long branchId,
            RedirectAttributes redirectAttributes
    ) {
        Roles role;

        try {
            String token = (String) session.getAttribute(authSessionKey);
            role = authService.validateToken(token).getRole();
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(role)) {
            ResponseInBoolean isDeleted = branchService.doDisableBranch(branchId);
            redirectAttributes.addFlashAttribute("status", isDeleted.isStatus());
            redirectAttributes.addFlashAttribute("message", isDeleted.getMessage());
            return "redirect:/branch";
        }
        return "redirect:/login";
    }
}
