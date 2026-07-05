package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.SidebarService;
import com.pos.posApps.Service.StaffService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("staff")
@AllArgsConstructor
public class StaffController {

    private AuthService authService;
    private StaffService staffService;
    private SidebarService sidebarService;

    @GetMapping
    public String displayStaff(HttpSession session, Model model, RedirectAttributes redirectAttributes){
        AccountEntity accountEntity;
        String token;
        try{
            token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
        }catch (Exception e){
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
    public String addStaff(HttpSession session, Model model, RedirectAttributes redirectAttributes, CreateStaffRequest req){
        AccountEntity accountEntity;
        String token;
        try{
            token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
        }catch (Exception e){
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
    public String displayStaffDetail(@PathVariable("karyawanId") Long karyawanId, HttpSession session, Model model, RedirectAttributes redirectAttributes){
        AccountEntity accountEntity;
        String token;
        try{
            token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
        }catch (Exception e){
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
    public String editStaff(@PathVariable("staffId") Long staffId, HttpSession session, RedirectAttributes redirectAttributes, EditStaffRequest req){
        AccountEntity accountEntity;
        String token;
        try{
            token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
        }catch (Exception e){
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accountEntity.getRole())) {
            ResponseInBoolean isEdited = staffService.editStaffData(staffId, req);
            redirectAttributes.addFlashAttribute("status", isEdited.isStatus());
            redirectAttributes.addFlashAttribute("message", isEdited.getMessage());
            System.out.println("all done");
            return "redirect:/staff/detail/"+staffId;
        }
        redirectAttributes.addFlashAttribute("status", false);
        redirectAttributes.addFlashAttribute("message", "Anda tidak punya akses!");
        return "redirect:/login";
    }

//    @PostMapping("/add")
//    public String addVehicle(String vehicleName, String vehicleBrand, String partNumber, HttpSession session, RedirectAttributes redirectAttributes){
//        AccountEntity accEntity;
//        ClientEntity clientData;
//        try{
//            String token = (String) session.getAttribute(authSessionKey);
//            accEntity = authService.validateToken(token);
//            clientData = accEntity.getClientEntity();
//            if (clientData.getClientId() == null) {
//                redirectAttributes.addFlashAttribute("status", true);
//                redirectAttributes.addFlashAttribute("message", "Sesi anda habis, harap login ulang");
//                return "redirect:/login";
//            }
//        }catch (Exception e){
//            redirectAttributes.addFlashAttribute("message", "Sesi anda habis, harap login ulang");
//            return "redirect:/login";
//        }
//
//        if (authService.hasAccessToModifyData(accEntity.getRole())) {
//            ResponseInBoolean isInserted = vehicleService.insertVehicle(vehicleName, vehicleBrand, partNumber);
//            redirectAttributes.addFlashAttribute("status", true);
//            redirectAttributes.addFlashAttribute("message", isInserted.getMessage());
//            return "redirect:/vehicle";
//        }
//        redirectAttributes.addFlashAttribute("status", true);
//        redirectAttributes.addFlashAttribute("message", "Anda tidak punya akses!");
//        return "redirect:/login";
//    }
//
//    @PostMapping("/edit")
//    public String editVehicle(Long vehicleId, String vehicleName, String vehicleBrand, String partNumber, HttpSession session, RedirectAttributes redirectAttributes){
//        AccountEntity accEntity;
//        ClientEntity clientData;
//        try{
//            String token = (String) session.getAttribute(authSessionKey);
//            accEntity = authService.validateToken(token);
//            clientData = accEntity.getClientEntity();
//            if (clientData.getClientId() == null) {
//                return "redirect:/login";
//            }
//        }catch (Exception e){
//            return "redirect:/login";
//        }
//
//        if (authService.hasAccessToModifyData(accEntity.getRole())) {
//            ResponseInBoolean isInserted = vehicleService.editVehicle(vehicleId, vehicleName, vehicleBrand, partNumber);
//            redirectAttributes.addFlashAttribute("status", isInserted.isStatus());
//            redirectAttributes.addFlashAttribute("message", isInserted.getMessage());
//            return "redirect:/vehicle";
//        }
//        redirectAttributes.addFlashAttribute("status", true);
//        redirectAttributes.addFlashAttribute("message", "Anda tidak punya akses");
//        return "redirect:/login";
//    }
//
//    @PostMapping("/delete/{vehicleId}")
//    public String deleteVehicle(@PathVariable("vehicleId") Long vehicleId, HttpSession session, RedirectAttributes redirectAttributes){
//        AccountEntity accEntity;
//        ClientEntity clientData;
//        try{
//            String token = (String) session.getAttribute(authSessionKey);
//            accEntity = authService.validateToken(token);
//            clientData = accEntity.getClientEntity();
//            if (clientData.getClientId() == null) {
//                return "redirect:/login";
//            }
//        }catch (Exception e){
//            return "redirect:/login";
//        }
//
//        if (authService.hasAccessToModifyData(accEntity.getRole())) {
//            boolean isDeleted = vehicleService.deleteVehicle(vehicleId);
//            if (isDeleted) {
//                redirectAttributes.addFlashAttribute("status", "success");
//                redirectAttributes.addFlashAttribute("message", "Data Deleted");
//                return "redirect:/vehicle";
//            }
//            redirectAttributes.addFlashAttribute("status", "failed");
//            redirectAttributes.addFlashAttribute("message", "Failed to delete data");
//            return "redirect:/vehicle";
//        }
//        return "redirect:/login";
//    }
}
