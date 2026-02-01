package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Entity.VehicleEntity;
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
@RequestMapping("inden")
@AllArgsConstructor
public class IndenController {
    private AuthService authService;
    private ClientService clientService;
    private SidebarService sidebarService;
    private IndenService indenService;
    private ProductService productService;
    private SupplierService supplierService;
    private VehicleService vehicleService;

    private int safeSize(Integer size) {
        return (size == null || size <= 0) ? 10 : size;
    }

    @GetMapping
    public String showInden(HttpSession session, Model model, String startDate, String endDate, String statusInden, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "300") Integer size, @RequestParam(required = false) String search) {
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

        Page<IndenDTO> indenData;
        ClientDTO clientSettingData = clientService.getClientSettings(clientId);
        if (statusInden != null && statusInden.isBlank()) {
            statusInden = null;
        }

        if (search == null || search.isEmpty()) {
            indenData = indenService.getIndenData(statusInden, inputStartDate, inputEndDate, PageRequest.of(page, size));
        } else {
            indenData = indenService.searchIndenData(statusInden, inputStartDate, inputEndDate, search, PageRequest.of(page, size));
        }
        model.addAttribute("statusInden", statusInden);
        System.out.println("Inden data : " + indenData.getContent());
        model.addAttribute("indenData", indenData.getContent());
        model.addAttribute("activePage", "indenRiwayat");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("search", search);

        Long totalElements = indenData.getTotalElements();

        Integer totalPages = indenData.getTotalPages();
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
            model.addAttribute("endData", page * size + indenData.getNumberOfElements());
        }

        model.addAttribute("settingData", clientSettingData);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_inden";
    }

    @GetMapping("/add")
    public String displayKasirInden(Model model, HttpSession session, Long indenId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size) {
        System.out.println("inden id : " + indenId);
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        Page<ProductDTO> productEntity = productService.getProductData(clientId, PageRequest.of(page, size), null, false);
        ClientDTO clientSettingData = clientService.getClientSettings(clientId);

        model.addAttribute("clientSettingData", clientSettingData);
        IndenDTO indenData = (indenId != null)
                ? indenService.getPenjualanDataById(indenId)
                : new IndenDTO();

        List<VehicleEntity> vehicleEntity = vehicleService.getVehicleList(null);
        model.addAttribute("vehicleData", vehicleEntity);
        List<SupplierEntity> supplierEntityList = supplierService.getSupplierList(clientId);
        model.addAttribute("indenData", indenData);
        model.addAttribute("supplierData", supplierEntityList);
        model.addAttribute("productData", productEntity.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productEntity.getTotalPages());
        model.addAttribute("settingData", clientSettingData);

        return "display_kasir_inden";
    }

    @PostMapping("/delete/{indenId}")
    public String deleteInden(@PathVariable("indenId") Long indenId, HttpSession session, RedirectAttributes redirectAttributes) {
        System.out.println("Entering delete");
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isDeleted = indenService.deleteInden(indenId);
            if (isDeleted) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Deleted");
                return "redirect:/inden";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to delete data");
            return "redirect:/inden";
        }
        return "redirect:/login";
    }

    @PostMapping("/update_status/{indenId}")
    public String updateStatusInden(@PathVariable("indenId") Long indenId, HttpSession session, RedirectAttributes redirectAttributes, String statusInden) {
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            ResponseForWhatsapp isUpdated = indenService.updateStatusInden(indenId, statusInden, accEntity);
            if (isUpdated.isStatus()) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", isUpdated.getMessage());
                return "redirect:/inden";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", isUpdated.getMessage());
            return "redirect:/inden";
        }
        return "redirect:/login";
    }

}
