package com.pos.posApps.Controller;

import com.pos.posApps.DTO.Dtos.CreateProductRequest;
import com.pos.posApps.DTO.Dtos.EditProductRequest;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.ProductEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("products")
@AllArgsConstructor
public class ProductController {
    private AuthService authService;
    private ProductService productService;

    @GetMapping
    public String showListProducts(HttpSession session, Model model) {
        String clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        List<ProductEntity> productEntity = productService.getProductData(clientId);

        System.out.println("product entity : " + productEntity);
        model.addAttribute("productData", productEntity);
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
                redirectAttributes.addFlashAttribute("status", "failed");
                redirectAttributes.addFlashAttribute("message", "Session Expired");
                return "redirect:/login";
            }
        } catch (Exception e) {
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isInserted = productService.insertProducts(req, clientData);
            if (isInserted) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Created");
                return "redirect:/products";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to Create Data");
            return "redirect:/products";
        }
        redirectAttributes.addFlashAttribute("status", "failed");
        redirectAttributes.addFlashAttribute("message", "Session Expired");
        return "redirect:/login";
    }

    @PostMapping("edit-products")
    public String editProducts(HttpSession session, EditProductRequest req, RedirectAttributes redirectAttributes) {
        AccountEntity accEntity;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            if(accEntity.getClientEntity().getClientId() == null){
                redirectAttributes.addFlashAttribute("status", "failed");
                redirectAttributes.addFlashAttribute("message", "Session Expired");
                return "redirect:/login";
            }
        } catch (Exception e) {
            return "redirect:/login";
        }

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = productService.editProducts(req);
            if (isEdited) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Edited");
                return "redirect:/products";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to edit data");
            return "redirect:/products";
        }
        return "redirect:/login";
    }

    @PostMapping("delete-products")
    public String deleteProducts(HttpSession session, String productId, RedirectAttributes redirectAttributes) {
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "redirect:/login";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = productService.deleteProducts(productId);
            if (isEdited) {
                redirectAttributes.addFlashAttribute("status", "success");
                redirectAttributes.addFlashAttribute("message", "Data Deleted");
                return "redirect:/products";
            }
            redirectAttributes.addFlashAttribute("status", "failed");
            redirectAttributes.addFlashAttribute("message", "Failed to delete data");
            return "redirect:/products";
        }
        return "redirect:/login";
    }
}
