package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
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
    private LaporanService laporanService;
    private SupplierService supplierService;
    private SidebarService sidebarService;

    @GetMapping("/pendapatan/periode")
    public String laporanPendapatan(HttpSession session, Model model, String startDate, String endDate, Long customerId, String filterOptions) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        // Default dates
        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank()) ? LocalDate.now().toString() : endDate;

        LocalDate parsedStart = LocalDate.parse(startDate);
        LocalDate parsedEnd = LocalDate.parse(endDate);

        LocalDateTime inputStartDate = parsedStart.atStartOfDay();
        LocalDateTime inputEndDate = parsedEnd.atTime(23, 59, 59);

        List<LaporanPenjualanPerWaktuDTO> laporanData = laporanService.getLaporanPenjualanDataByPeriode(clientId, inputStartDate, inputEndDate, customerId, filterOptions);
        List<CustomerEntity> customerData = customerService.getCustomerList(clientId);
        model.addAttribute("customerData", customerData);
        model.addAttribute("customerId", customerId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("filterOptions", filterOptions);
        model.addAttribute("laporanData", laporanData);
        model.addAttribute("activePage", "pendapatanPeriode");
        model.addAttribute("activeSubPage", "pendapatanPeriode");
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);

        // Add any necessary data
        return "display_laporan_pendapatan_per_periode"; // Thymeleaf template
    }

    @GetMapping("/pendapatan/pelanggan")
    public String laporanPendapatanPelanggan(HttpSession session, Model model, String startDate, String endDate) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        // Default dates
        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank()) ? LocalDate.now().toString() : endDate;

        LocalDate parsedStart = LocalDate.parse(startDate);
        LocalDate parsedEnd = LocalDate.parse(endDate);

        LocalDateTime inputStartDate = parsedStart.atStartOfDay();
        LocalDateTime inputEndDate = parsedEnd.atTime(23, 59, 59);

        List<LaporanPenjualanPerPelangganDTO> laporanData = laporanService.getLaporanPenjualanDataByCustomer(clientId, inputStartDate, inputEndDate);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("laporanData", laporanData);
        model.addAttribute("activePage", "pendapatanPelanggan");
        model.addAttribute("activeSubPage", "pendapatanPelanggan");
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);

        // Add any necessary data
        return "display_laporan_pendapatan_per_pelanggan"; // Thymeleaf template
    }

    // ======= Laporan Pengeluaran =======
    @GetMapping("/pengeluaran/periode")
    public String laporanPengeluaran(HttpSession session, Model model, String startDate, String endDate, Long customerId, String filterOptions) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        // Default dates
        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank()) ? LocalDate.now().toString() : endDate;

        LocalDate parsedStart = LocalDate.parse(startDate);
        LocalDate parsedEnd = LocalDate.parse(endDate);

        LocalDateTime inputStartDate = parsedStart.atStartOfDay();
        LocalDateTime inputEndDate = parsedEnd.atTime(23, 59, 59);

        List<LaporanPembelianPerWaktuDTO> laporanData = laporanService.getLaporanPengeluaranDataByPeriode(clientId, inputStartDate, inputEndDate, customerId, filterOptions);
        List<SupplierEntity> supplierData = supplierService.getSupplierList(clientId);
        model.addAttribute("supplierData", supplierData);
        model.addAttribute("customerId", customerId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("filterOptions", filterOptions);
        model.addAttribute("laporanData", laporanData);
        model.addAttribute("activePage", "pengeluaranPeriode");
        model.addAttribute("activeSubPage", "pengeluaranPeriode");
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);

        // Add any necessary data
        return "display_laporan_pengeluaran_per_periode"; // Thymeleaf template
    }

    @GetMapping("/pengeluaran/pelanggan")
    public String laporanPengeluaranPelanggan(HttpSession session, Model model, String startDate, String endDate) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        // Default dates
        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank()) ? LocalDate.now().toString() : endDate;

        LocalDate parsedStart = LocalDate.parse(startDate);
        LocalDate parsedEnd = LocalDate.parse(endDate);

        LocalDateTime inputStartDate = parsedStart.atStartOfDay();
        LocalDateTime inputEndDate = parsedEnd.atTime(23, 59, 59);

        List<LaporanPembelianPerPelangganDTO> laporanData = laporanService.getLaporanPembelianDataByCustomer(clientId, inputStartDate, inputEndDate);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("laporanData", laporanData);
        model.addAttribute("activePage", "pengeluaranPelanggan");
        model.addAttribute("activeSubPage", "pengeluaranPelanggan");
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);

        // Add any necessary data
        return "display_laporan_pengeluaran_per_pelanggan"; // Thymeleaf template
    }

    @GetMapping("/nilai_persediaan")
    public String laporanNilaiPersediaan(HttpSession session, Model model, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        Page<LaporanNilaiPersediaanDTO> laporanData = laporanService.getLaporanNilaiPersediaan(clientId, PageRequest.of(page, size));

        Integer totalPages = laporanData.getTotalPages();
        Integer start = Math.max(0, page - 2);
        Integer end = Math.min(totalPages - 1, page + 2);

        model.addAttribute("laporanData", laporanData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("size", size);
        model.addAttribute("startData", page * size + 1);
        model.addAttribute("endData", page * size + laporanData.getNumberOfElements());
        model.addAttribute("totalData", laporanData.getTotalElements());

        model.addAttribute("activePage", "nilaiPersediaan");
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);

        return "display_laporan_nilai_persediaan_barang"; // Thymeleaf template
    }

    @GetMapping("/nilai_persediaan/view-pdf")
    public void viewLaporanNilaiPersediaanPDF(HttpSession session, HttpServletResponse response) throws IOException {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            response.sendRedirect("/login");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=laporan_nilai_persediaan.pdf");

        try {
            laporanService.exportLaporanNilaiPersediaanStream(clientId, response.getOutputStream());
        } catch (Exception ex) {
            ex.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Gagal membuat PDF");
        }
    }


}
