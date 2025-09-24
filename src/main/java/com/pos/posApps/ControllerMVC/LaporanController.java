package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.LaporanPerPelangganDTO;
import com.pos.posApps.DTO.Dtos.LaporanPerWaktuDTO;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.CustomerService;
import com.pos.posApps.Service.PenjualanService;
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
@RequestMapping("laporan")
@AllArgsConstructor
public class LaporanController {

    private AuthService authService;
    private CustomerService customerService;
    private PenjualanService penjualanService;

    @GetMapping("/pendapatan/periode")
    public String laporanPendapatan(HttpSession session, Model model, String startDate, String endDate, Long customerId, String filterOptions){
        Long clientId;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            return "redirect:/login";
        }

        // Default dates
        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank()) ? LocalDate.now().toString() : endDate;

        LocalDate parsedStart = LocalDate.parse(startDate);
        LocalDate parsedEnd = LocalDate.parse(endDate);

        LocalDateTime inputStartDate = parsedStart.atStartOfDay();
        LocalDateTime inputEndDate = parsedEnd.atTime(23, 59, 59);

        List<LaporanPerWaktuDTO> laporanData = penjualanService.getLaporanPenjualanDataByPeriode(clientId, inputStartDate, inputEndDate, customerId, filterOptions);
        List<CustomerEntity> customerData = customerService.getCustomerList(clientId);
        System.out.println("Data : " + laporanData);
        model.addAttribute("customerData", customerData);
        model.addAttribute("customerId", customerId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("filterOptions", filterOptions);
        model.addAttribute("laporanData", laporanData);
        model.addAttribute("activePage", "pendapatanPeriode");
        model.addAttribute("activeSubPage", "pendapatanPeriode"); // Add this!

        // Add any necessary data
        return "display_laporan_pendapatan_per_periode"; // Thymeleaf template
    }

    @GetMapping("/pendapatan/pelanggan")
    public String laporanPendapatanPelanggan(HttpSession session, Model model, String startDate, String endDate){
        Long clientId;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            return "redirect:/login";
        }

        // Default dates
        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank()) ? LocalDate.now().toString() : endDate;

        LocalDate parsedStart = LocalDate.parse(startDate);
        LocalDate parsedEnd = LocalDate.parse(endDate);

        LocalDateTime inputStartDate = parsedStart.atStartOfDay();
        LocalDateTime inputEndDate = parsedEnd.atTime(23, 59, 59);

        List<LaporanPerPelangganDTO> laporanData = penjualanService.getLaporanPenjualanDataByCustomer(clientId, inputStartDate, inputEndDate);
        System.out.println("Data : " + laporanData);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("laporanData", laporanData);
        model.addAttribute("activePage", "pendapatanPelanggan");
        model.addAttribute("activeSubPage", "pendapatanPelanggan"); // Add this!

        // Add any necessary data
        return "display_laporan_pendapatan_per_pelanggan"; // Thymeleaf template
    }

    // ======= Laporan Pengeluaran =======
    @GetMapping("/pengeluaran")
    public String laporanPengeluaran(Model model) {
        model.addAttribute("activePage", "laporanPengeluaranPerTgl");
        return "display_laporan_pengeluaran";
    }
}
