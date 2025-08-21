package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.PenjualanService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping
    public String showPenjualan(HttpSession session, Model model, String startDate, String endDate){
        System.out.println("STart date : " +startDate);
        System.out.println("end date : " +endDate);
        String clientId;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            System.out.println("catch penjualan");
            return "redirect:/login";
        }

        startDate = (startDate == null || startDate.isEmpty()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isEmpty())? LocalDate.now().toString() : endDate;


        LocalDateTime inputStartDate = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime inputEndDate = LocalDate.parse(endDate).atTime(23, 59, 59);


        List<PenjualanDTO> penjualanData = penjualanService.getPenjualanData(clientId, inputStartDate, inputEndDate);
        System.out.println("penjualan data : " + penjualanData);
        model.addAttribute("penjualanData", penjualanData);
        model.addAttribute("activePage", "penjualan");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "display_penjualan";
    }

    @PostMapping("/delete/{transactionId}")
    public String deletePenjualan(@PathVariable("transactionId") String transactionId, HttpSession session, RedirectAttributes redirectAttributes){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isDeleted = penjualanService.deletePenjualan(transactionId, clientData.getClientId());
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
