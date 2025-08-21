package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.PembelianService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("pembelian")
@AllArgsConstructor
public class PembelianController {
    private AuthService authService;
    private PembelianService pembelianService;

    @GetMapping
    public String showPembelian(HttpSession session, Model model, String startDate, String endDate){
        System.out.println("STart date : " +startDate);
        System.out.println("end date : " +endDate);
        Long clientId;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            System.out.println("catch pembelian");
            return "redirect:/login";
        }

        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank())? LocalDate.now().toString() : endDate;


        LocalDateTime inputStartDate = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime inputEndDate = LocalDate.parse(endDate).atTime(23, 59, 59);


        List<PembelianDTO> pembelianData = pembelianService.getPembelianData(clientId, inputStartDate, inputEndDate);
        System.out.println("pembelian data : " + pembelianData);
        model.addAttribute("pembelianData", pembelianData);
        model.addAttribute("activePage", "pembelian");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "display_pembelian";
    }
}
