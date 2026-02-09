package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.*;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;

@Controller
@RequestMapping("branch")
@AllArgsConstructor
public class BranchController {

    private AccountService accountService;
    private AuthService authService;
    private SidebarService sidebarService;
    private BranchService branchService;
    private CustomerService customerService;
    private ClientService clientService;
    private PenjualanService penjualanService;
    private ProductService productService;

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
        model.addAttribute("activePage", "listCabang");
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

    private int safeSize(Integer size) {
        return (size == null || size <= 0) ? 10 : size;
    }

    @GetMapping("/transfer/riwayat")
    public String showRiwayatTransferStok(HttpSession session, Model model, String startDate, String endDate, Long customerId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "300") Integer size, @RequestParam(required = false) String search) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().withDayOfMonth(1).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank()) ? LocalDate.now().toString() : endDate;

        LocalDateTime inputStartDate = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime inputEndDate = LocalDate.parse(endDate).atTime(23, 59, 59);

        Page<PenjualanDTO> penjualanData;
        List<CustomerEntity> customerData = branchService.getAllCabangToko();
        ClientDTO clientSettingData = clientService.getClientSettings(clientId);

        List<Long> allTokoCabangId;
        if(customerId != null){
            allTokoCabangId = listOf(customerId);
        }else{
            allTokoCabangId = customerData.stream().filter(Objects::nonNull).map(CustomerEntity::getCustomerId).toList();
        }

        if (search == null || search.isEmpty()) {
            penjualanData = penjualanService.getPenjualanData(clientId, inputStartDate, inputEndDate, allTokoCabangId, PageRequest.of(page, size));
        } else {
            penjualanData = penjualanService.searchPenjualanData(clientId, inputStartDate, inputEndDate, allTokoCabangId, search, PageRequest.of(page, size));
        }

        model.addAttribute("transferData", penjualanData.getContent());
        model.addAttribute("customerId", customerId);
        model.addAttribute("customerData", customerData);
        model.addAttribute("activePage", "riwayatTransferStock");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("search", search);

        Long totalElements = penjualanData.getTotalElements();

        Integer totalPages = penjualanData.getTotalPages();
        if (totalPages == 0) {
            totalPages = 1;
        }

        Integer start = Math.max(0, page - 2);
        Integer end = Math.min(totalPages - 1, page + 2);
        size = safeSize(size);
        model.addAttribute("size", size);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("totalData", totalElements);

        if (totalElements == 0) {
            model.addAttribute("startData", 0);
            model.addAttribute("endData", 0);
        } else {
            model.addAttribute("startData", page * size + 1);
            model.addAttribute("endData", page * size + penjualanData.getNumberOfElements());
        }

        model.addAttribute("settingData", clientSettingData);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_transfer_data";
    }

    @GetMapping("/transfer/kasir")
    public String displayKasirTransfer(Model model, HttpSession session, Long transactionId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size) {
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        Page<ProductDTO> productEntity = productService.getProductData(clientId, PageRequest.of(page, size), null, false);
        List<CustomerEntity> customerEntities = branchService.getAllCabangToko();
        ClientDTO clientSettingData = clientService.getClientSettings(clientId);
        model.addAttribute("clientSettingData", clientSettingData);
        PenjualanDTO penjualanData = (transactionId != null)
                ? penjualanService.getPenjualanDataById(clientId, transactionId)
                : new PenjualanDTO();

        model.addAttribute("penjualanData", penjualanData);
        model.addAttribute("activePage", "kasir");
        model.addAttribute("productData", productEntity.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productEntity.getTotalPages());
        model.addAttribute("customerData", customerEntities);
        model.addAttribute("settingData", clientSettingData);

        return "display_kasir_transfer_stok";
    }

    @PostMapping("/transfer/delete/{transactionId}")
    public String deletePenjualan(@PathVariable("transactionId") Long transactionId, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isDeleted = penjualanService.deletePenjualan(transactionId, clientData);
            if (isDeleted) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Deleted");
                return "redirect:/branch/transfer/riwayat";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to delete data");
            return "redirect:/branch/transfer/riwayat";
        }
        return "redirect:/login";
    }
}
