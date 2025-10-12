package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ProductService;
import com.pos.posApps.Service.SidebarService;
import com.pos.posApps.Service.SupplierService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
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
@RequestMapping("products")
@AllArgsConstructor
public class ProductController {
    private AuthService authService;
    private ProductService productService;
    private SupplierService supplierService;
    private SidebarService sidebarService;

    @GetMapping
    public String showListProducts(HttpSession session, Model model) {
        Long clientId;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);

        List<ProductDTO> productEntity = productService.getProductData(clientId);
        List<SupplierEntity> supplierEntity = supplierService.getSupplierList(clientId);
        model.addAttribute("productData", productEntity);
        model.addAttribute("supplierData", supplierEntity);
        model.addAttribute("activePage", "masterBarang");

        return "display_products";
    }

    @PostMapping("/add")
    public String addProducts(HttpSession session, CreateProductRequest req, RedirectAttributes redirectAttributes) {
        AccountEntity accEntity;
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            clientData = accEntity.getClientEntity();
            if (clientData.getClientId() == null) {
                redirectAttributes.addFlashAttribute("status", true);
                redirectAttributes.addFlashAttribute("message", "Session Expired");
                return "redirect:/login";
            }
        } catch (Exception e) {
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            ResponseInBoolean isInserted = productService.insertProducts(req, clientData);
            if (isInserted.isStatus()) {
                redirectAttributes.addFlashAttribute("status", true);
                redirectAttributes.addFlashAttribute("message", "Data Created");
                return "redirect:/products";
            }
            redirectAttributes.addFlashAttribute("status", true);
            redirectAttributes.addFlashAttribute("message", "Failed to Create Data");
            return "redirect:/products";
        }
        redirectAttributes.addFlashAttribute("status", true);
        redirectAttributes.addFlashAttribute("message", "Session Expired");
        return "redirect:/login";
    }

    @PostMapping("/edit")
    public String editProducts(HttpSession session, EditProductRequest req, RedirectAttributes redirectAttributes) {
        AccountEntity accEntity;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            if(accEntity.getClientEntity().getClientId() == null){
                redirectAttributes.addFlashAttribute("status", true);
                redirectAttributes.addFlashAttribute("message", "Session Expired");
                return "redirect:/login";
            }
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = productService.editProducts(req, accEntity.getClientEntity());
            if (isEdited) {
                redirectAttributes.addFlashAttribute("status", true);
                redirectAttributes.addFlashAttribute("message", "Data Edited");
                return "redirect:/products";
            }
            redirectAttributes.addFlashAttribute("status", true);
            redirectAttributes.addFlashAttribute("message", "Failed to edit data");
            return "redirect:/products";
        }
        return "redirect:/login";
    }

    @PostMapping("delete/{productId}")
    public String deleteProducts(HttpSession session, @PathVariable("productId") Long productId, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = productService.deleteProducts(productId);
            if (isEdited) {
                redirectAttributes.addFlashAttribute("status", true);
                redirectAttributes.addFlashAttribute("message", "Data Deleted");
                return "redirect:/products";
            }
            redirectAttributes.addFlashAttribute("status", true);
            redirectAttributes.addFlashAttribute("message", "Failed to delete data");
            return "redirect:/products";
        }
        return "redirect:/login";
    }

    @GetMapping("/kartu_stok")
    public String showKartuStokPage(HttpSession session, Model model, String startDate, String endDate, Long productId) {
        Long clientId;
        boolean isShowDetail = true;
        String token;
        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank())? LocalDate.now().toString() : endDate;


        LocalDateTime inputStartDate = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime inputEndDate = LocalDate.parse(endDate).atTime(23, 59, 59);

        List<ProductDTO> productEntity = productService.getProductData(clientId);
        List<StockMovementsDTO> stokData = productService.getStockMovementData(clientId, productId, inputStartDate, inputEndDate);
        Long stockAwal = productService.getStockAwalProduct(productId, inputStartDate);
        model.addAttribute("isShowDetail", isShowDetail);
        model.addAttribute("productData", productEntity);
        model.addAttribute("activePage", "kartuStok");
        model.addAttribute("kartuStok", stokData);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("stockAwal", stockAwal);
        model.addAttribute("selectedItemId", productId);
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);
        model.addAttribute("sidebarData", sidebarData);
        return "display_kartuStok";
    }
}
