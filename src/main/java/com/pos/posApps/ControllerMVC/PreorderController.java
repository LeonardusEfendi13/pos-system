package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.PreorderDTO;
import com.pos.posApps.DTO.Dtos.ProductDTO;
import com.pos.posApps.DTO.Dtos.SidebarDTO;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Service.*;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("preorder")
@AllArgsConstructor
@RequiredArgsConstructor
public class PreorderController {
    @Autowired
    private AuthService authService;

    @Autowired
    private PreorderService preorderService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ProductService productService;

    @Autowired
    private SidebarService sidebarService;

    @Autowired
    private VehicleService vehicleService;

    private int safeSize(Integer size) {
        return (size == null || size <= 0) ? 10 : size;
    }

    @GetMapping
    public String showListPreorder(Long supplierId, String startDate, String endDate, HttpSession session, Model model, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "200") Integer size, @RequestParam(required = false) String search) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }
        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank()) ? LocalDate.now().toString() : endDate;

        LocalDateTime inputStartDate = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime inputEndDate = LocalDate.parse(endDate).atTime(23, 59, 59);

        Page<PreorderEntity> preorderEntity;
        List<SupplierEntity> supplierData = supplierService.getSupplierList(clientId);

        if (search == null || search.isEmpty()) {
            preorderEntity = preorderService.getPreorderData(clientId, supplierId, inputStartDate, inputEndDate, PageRequest.of(page, size));
        } else {
            preorderEntity = preorderService.searchPreorderData(clientId, supplierId, inputStartDate, inputEndDate, search, PageRequest.of(page, size));
        }

        model.addAttribute("supplierId", supplierId);
        model.addAttribute("supplierData", supplierData);
        model.addAttribute("preorderData", preorderEntity.getContent());
        model.addAttribute("activePage", "preorderRiwayat");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        long totalElements = preorderEntity.getTotalElements();

        int totalPages = preorderEntity.getTotalPages();
        if (totalPages == 0) {
            totalPages = 1;
        }

        Integer start = Math.max(0, page - 2);
        Integer end = Math.min(totalPages - 1, page + 2);
        size = safeSize(size);
        model.addAttribute("size", size);
        model.addAttribute("start", start);
        model.addAttribute("end", end);model.addAttribute("totalData", totalElements);

        if (totalElements == 0) {
            model.addAttribute("startData", 0);
            model.addAttribute("endData", 0);
        } else {
            model.addAttribute("startData", page * size + 1);
            model.addAttribute("endData", page * size + preorderEntity.getNumberOfElements());
        }

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_preorder";
    }

    @GetMapping("/tambah")
    public String addPreorder(HttpSession session, Long preorderId, Model model, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size) {
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            return "redirect:/login";
        }
        Page<ProductDTO> productEntity = productService.getProductData(clientData.getClientId(), PageRequest.of(page, size), null, false);
        List<SupplierEntity> supplierEntities = supplierService.getSupplierList(clientData.getClientId());

        PreorderDTO preorderData = new PreorderDTO();
        if (preorderId != null) {
            preorderData = preorderService.getPreorderDataById(clientData.getClientId(), preorderId);
        }
        List<VehicleEntity> vehicleEntity = vehicleService.getVehicleList(null);
        model.addAttribute("vehicleData", vehicleEntity);
        model.addAttribute("preorderData", preorderData);
        model.addAttribute("activePage", "preorderTambah");
        model.addAttribute("productData", productEntity.getContent());
        model.addAttribute("supplierData", supplierEntities);
        return "display_kasir_preorder";
    }

    @GetMapping("/understock")
    public String getUnderstokData(HttpSession session, Model model, Long supplierId){
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }
        List<SupplierEntity> supplierEntities = supplierService.getSupplierList(clientId);
        String supplierName = supplierService.getSupplierDataById(supplierId, clientId);
        List<ProductDTO> productData = Collections.emptyList();
        if(supplierId != null){
            productData = productService.getUnderstockProductData(clientId, supplierId);
        }
        model.addAttribute("productData", productData);
        model.addAttribute("supplierData", supplierEntities);
        model.addAttribute("activePage", "preorderUnderstock");
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("supplierName", supplierName);
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_understock";
    }

    @PostMapping("/delete/{preorderId}")
    public String deletePurchasing(@PathVariable("preorderId") Long preorderId, HttpSession session, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isDeleted = preorderService.deletePreorder(preorderId, clientData);
            if (isDeleted) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Deleted");
                return "redirect:/preorder";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to delete data");
            return "redirect:/preorder";
        }
        return "redirect:/login";
    }
}
