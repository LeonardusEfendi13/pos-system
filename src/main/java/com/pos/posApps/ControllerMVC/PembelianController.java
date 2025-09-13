package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.PembelianService;
import com.pos.posApps.Service.ProductService;
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
@RequestMapping("pembelian")
@AllArgsConstructor
public class PembelianController {
    private AuthService authService;
    private PembelianService pembelianService;
    private SupplierService supplierService;
    private ProductService productService;

    @GetMapping
    public String showPembelian(HttpSession session, Model model, String startDate, String endDate, Long supplierId){
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

        List<PembelianDTO> pembelianData = pembelianService.getPembelianData(clientId, inputStartDate, inputEndDate, supplierId);
        List<SupplierEntity> supplierData = supplierService.getSupplierList(clientId);
        System.out.println("pembelian data : " + pembelianData);
        model.addAttribute("pembelianData", pembelianData);
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("supplierData", supplierData);
        model.addAttribute("activePage", "pembelianRiwayat");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "display_pembelian";
    }

    @GetMapping("/tambah")
    public String displayKasir(Model model, HttpSession session, Long pembelianId) {
        System.out.println("display kasir edit : "+ pembelianId);
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }
        List<ProductDTO> productEntity = productService.getProductData(clientId);
        List<SupplierEntity> supplierEntities = supplierService.getSupplierList(clientId);

        PembelianDTO pembelianData = new PembelianDTO();
        if (pembelianId != null) {
            pembelianData = pembelianService.getPembelianDataById(clientId, pembelianId);
        }
        System.out.println("Pembelian data untuk edit : " + pembelianData );
        model.addAttribute("pembelianData", pembelianData);
        model.addAttribute("activePage", "pembelianTambah");
        model.addAttribute("productData", productEntity);
        model.addAttribute("supplierData", supplierEntities);
        return "display_kasir_pembelian";
    }

    @PostMapping("/delete/{purchasingId}")
    public String deletePurchasing(@PathVariable("purchasingId") Long puchasingId, HttpSession session, RedirectAttributes redirectAttributes){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isDeleted = pembelianService.deletePurchasing(puchasingId, clientData.getClientId());
            if (isDeleted) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Deleted");
                return "redirect:/pembelian";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to delete data");
            return "redirect:/pembelian";
        }
        return "redirect:/login";
    }
}
