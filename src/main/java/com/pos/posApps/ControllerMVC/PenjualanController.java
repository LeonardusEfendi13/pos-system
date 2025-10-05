package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.ClientDTO;
import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.DTO.Dtos.SidebarDTO;
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

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("penjualan")
@AllArgsConstructor
public class PenjualanController {
    private AuthService authService;
    private PenjualanService penjualanService;
    private CustomerService customerService;
    private ClientService clientService;
    private SidebarService sidebarService;

    @GetMapping
    public String showPenjualan(HttpSession session, Model model, String startDate, String endDate, Long customerId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size){
        Long clientId;
        String token;
        try{
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            return "redirect:/login";
        }

        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank())? LocalDate.now().toString() : endDate;


        LocalDateTime inputStartDate = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime inputEndDate = LocalDate.parse(endDate).atTime(23, 59, 59);


        Page<PenjualanDTO> penjualanData = penjualanService.getPenjualanData(clientId, inputStartDate, inputEndDate, customerId, PageRequest.of(page, size));
        List<CustomerEntity> customerData = customerService.getCustomerList(clientId);
        ClientDTO clientSettingData = clientService.getClientSettings(clientId);
        model.addAttribute("penjualanData", penjualanData);
        model.addAttribute("customerId", customerId);
        model.addAttribute("customerData", customerData);
        model.addAttribute("activePage", "penjualan");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        Integer totalPages = penjualanData.getTotalPages();
        Integer start = Math.max(0, page - 2);
        Integer end = Math.min(totalPages - 1, page + 2);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("startData", page * size + 1);
        model.addAttribute("endData", page * size + penjualanData.getNumberOfElements());
        model.addAttribute("totalData", penjualanData.getTotalElements());
        model.addAttribute("settingData", clientSettingData);
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_penjualan";
    }

    @PostMapping("/delete/{transactionId}")
    public String deletePenjualan(@PathVariable("transactionId") Long transactionId, HttpSession session, RedirectAttributes redirectAttributes){
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
                return "redirect:/penjualan";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to delete data");
            return "redirect:/penjualan";
        }
        return "redirect:/login";
    }

}
