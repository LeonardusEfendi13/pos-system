package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AttendanceService;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.SidebarService;
import com.pos.posApps.Service.StaffService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("staff")
@AllArgsConstructor
public class StaffController {

    private AuthService authService;
    private StaffService staffService;
    private SidebarService sidebarService;
    private AttendanceService attendanceService;

    @GetMapping
    public String displayStaff(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        AccountEntity accountEntity;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accountEntity.getRole())) {
            SidebarDTO sidebarData = sidebarService.getSidebarData(accountEntity.getClientEntity().getClientId(), token);
            DashboardKaryawanDTO dashboardKaryawanDTO = staffService.getDashboardData();
            model.addAttribute("sidebarData", sidebarData);
            model.addAttribute("activePage", "dashboardKaryawan");
            model.addAttribute("dashboardData", dashboardKaryawanDTO);
            return "display_dashboard_karyawan";
        }
        redirectAttributes.addFlashAttribute("status", true);
        redirectAttributes.addFlashAttribute("message", "Anda tidak punya akses!");
        return "redirect:/login";
    }

    @PostMapping("/add")
    public String addStaff(HttpSession session, Model model, RedirectAttributes redirectAttributes, CreateStaffRequest req) {
        AccountEntity accountEntity;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accountEntity.getRole())) {
            SidebarDTO sidebarData = sidebarService.getSidebarData(accountEntity.getClientEntity().getClientId(), token);
            ResponseInBoolean isAdded = staffService.addStaffData(req);
            redirectAttributes.addFlashAttribute("status", isAdded.isStatus());
            redirectAttributes.addFlashAttribute("message", isAdded.getMessage());
            redirectAttributes.addFlashAttribute("sidebarData", sidebarData);
            System.out.println("all done");
            return "redirect:/staff";
        }
        redirectAttributes.addFlashAttribute("status", false);
        redirectAttributes.addFlashAttribute("message", "Anda tidak punya akses!");
        return "redirect:/login";
    }

    @GetMapping("/detail/{karyawanId}")
    public String displayStaffDetail(@PathVariable("karyawanId") Long karyawanId, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        AccountEntity accountEntity;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accountEntity.getRole())) {
            SidebarDTO sidebarData = sidebarService.getSidebarData(accountEntity.getClientEntity().getClientId(), token);
            StaffDTO staffData = staffService.getStaffDetail(karyawanId);
            model.addAttribute("sidebarData", sidebarData);
            model.addAttribute("activePage", "dashboardKaryawan");
            model.addAttribute("staff", staffData);
            return "display_detail_karyawan";
        }
        redirectAttributes.addFlashAttribute("status", true);
        redirectAttributes.addFlashAttribute("message", "Anda tidak punya akses!");
        return "redirect:/login";

    }

    @PostMapping("/edit/{staffId}")
    public String editStaff(@PathVariable("staffId") Long staffId, HttpSession session, RedirectAttributes redirectAttributes, EditStaffRequest req) {
        AccountEntity accountEntity;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accountEntity.getRole())) {
            ResponseInBoolean isEdited = staffService.editStaffData(staffId, req);
            redirectAttributes.addFlashAttribute("status", isEdited.isStatus());
            redirectAttributes.addFlashAttribute("message", isEdited.getMessage());
            System.out.println("all done");
            return "redirect:/staff/detail/" + staffId;
        }
        redirectAttributes.addFlashAttribute("status", false);
        redirectAttributes.addFlashAttribute("message", "Anda tidak punya akses!");
        return "redirect:/login";
    }

    @GetMapping("/attendance")
    public String getAttendancePage(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        AccountEntity accountEntity;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accountEntity.getRole())) {
            SidebarDTO sidebarData = sidebarService.getSidebarData(accountEntity.getClientEntity().getClientId(), token);

            // Jika filter kosong, default ke bulan dan tahun saat ini
            LocalDate today = LocalDate.now();
            int selectedMonth = (month == null) ? today.getMonthValue() : month;
            int selectedYear = (year == null) ? today.getYear() : year;

            // Ambil tanggal awal bulan dan akhir bulan secara dinamis
            LocalDate startLocalDate = LocalDate.of(selectedYear, selectedMonth, 1);
            LocalDate endLocalDate = startLocalDate.withDayOfMonth(startLocalDate.lengthOfMonth());

            LocalDateTime inputStartDate = startLocalDate.atStartOfDay();
            LocalDateTime inputEndDate = endLocalDate.atTime(23, 59, 59);

            // Panggil service yang sudah diperbaiki sebelumnya
            List<AttendanceHistoryDTO> data = attendanceService.getMonthlyAttendanceMatrix(inputStartDate, inputEndDate);
            List<StaffDTO> staffData = staffService.getStaffData();

            // Jangan lupa pastikan data dilempar ke model dengan key 'matrixData' sesuai di HTML
            System.out.println("Matrik data : " + data);
            model.addAttribute("matrixData", data);
            model.addAttribute("activePage", "absensiKaryawan");
            model.addAttribute("sidebarData", sidebarData);
            model.addAttribute("listStaff", staffData);
            model.addAttribute("listStaffActive", staffData.stream().filter(staff-> staff.getTanggalResign() == null).toList());
            model.addAttribute("currentMonth", selectedMonth);
            model.addAttribute("currentYear", selectedYear);

            // Mengirim balik parameter bulan & tahun agar select option di HTML tetap bertahan (selected)
            model.addAttribute("currentMonth", selectedMonth);
            model.addAttribute("currentYear", selectedYear);

            return "display_attendance_staff";
        }

        redirectAttributes.addFlashAttribute("status", false);
        redirectAttributes.addFlashAttribute("message", "Anda tidak punya akses!");
        return "redirect:/login";
    }

    @PostMapping("/attendance/save-batch")
    public String saveBatchAttendance(HttpSession session,
                                      @ModelAttribute AttendanceBatchForm form,
                                      RedirectAttributes redirectAttributes) {
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            authService.validateToken(token);
        } catch (Exception e) {
            return "redirect:/login";
        }

        try {
            // Panggil service untuk menyimpan data secara massal
            ResponseInBoolean isSaved = attendanceService.saveBatchAttendance(form);
            redirectAttributes.addFlashAttribute("status", isSaved.isStatus());
            redirectAttributes.addFlashAttribute("message", isSaved.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("status", false);
            redirectAttributes.addFlashAttribute("message", "Gagal menyimpan absensi: " + e.getMessage());
        }

        // Kembalikan ke halaman utama absensi
        return "redirect:/staff/attendance";
    }

    @PostMapping("/attendance/update-single")
    public String updateSingleAttendance(
            @RequestParam("staffId") Long staffId,
            @RequestParam("tanggal") String tanggal, // Menerima angka hari, misal "18"
            @RequestParam("status") String status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            authService.validateToken(token);
        } catch (Exception e) {
            return "redirect:/login";
        }
        try {
            LocalDate tanggalFinal = LocalDate.parse(tanggal);
            System.out.println("otw service");
            ResponseInBoolean isUpdated = attendanceService.updateSingleStaff(staffId, tanggalFinal, status);
            redirectAttributes.addFlashAttribute("status", isUpdated.isStatus());
            redirectAttributes.addFlashAttribute("message", isUpdated.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("status", false);
            redirectAttributes.addFlashAttribute("message", "Gagal menyimpan absensi: " + e.getMessage());
        }
        // Kembalikan ke halaman utama absensi
        return "redirect:/staff/attendance";
    }

    @GetMapping("/payroll")
    public String displayPayroll(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        AccountEntity accountEntity;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accountEntity.getRole())) {
            SidebarDTO sidebarData = sidebarService.getSidebarData(accountEntity.getClientEntity().getClientId(), token);
            DashboardKaryawanDTO dashboardKaryawanDTO = staffService.getDashboardData();
            model.addAttribute("sidebarData", sidebarData);
            model.addAttribute("activePage", "dashboardKaryawan");
            model.addAttribute("dashboardData", dashboardKaryawanDTO);
            return "payroll";
        }
        redirectAttributes.addFlashAttribute("status", true);
        redirectAttributes.addFlashAttribute("message", "Anda tidak punya akses!");
        return "redirect:/login";

    }
}
