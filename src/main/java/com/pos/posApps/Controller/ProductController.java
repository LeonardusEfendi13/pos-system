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

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("api/v1/products")
@AllArgsConstructor
public class ProductController {
    private AuthService authService;
    private ProductService productService;

    @GetMapping("/list-products")
    public String showListProducts(HttpSession session, Model model){
        String token = (String) session.getAttribute(authSessionKey);
        String clientId = authService.validateToken(token).getClientEntity().getClientId();
        if(clientId == null){
            System.out.println("Failed");
            return "login";
        }
        List<ProductEntity> productEntity = productService.getProductData(clientId);
        model.addAttribute("productEntity", productEntity);
        return "display_products";
    }

    @PostMapping("/add-products")
    public String addProducts(HttpSession session, CreateProductRequest req) {
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "401";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isInserted = productService.insertProducts(req, clientData);
            if (isInserted) {
                return "200";
            }
            return "500";
        }
        return "401";
    }

    @PostMapping("edit-products")
    public String editProducts(HttpSession session, EditProductRequest req){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "401";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = productService.editProducts(req);
            if(isEdited){
                return "200";
            }
            return "500";
        }
        return "401";
    }

    @PostMapping("delete-products")
    public String deleteProducts(HttpSession session, String productId){
        String token = (String) session.getAttribute(authSessionKey);
        AccountEntity accEntity = authService.validateToken(token);
        ClientEntity clientData = accEntity.getClientEntity();
        if (clientData.getClientId() == null) {
            System.out.println("No Access to products");
            return "401";
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            boolean isEdited = productService.deleteProducts(productId);
            if(isEdited){
                return "200";
            }
            return "500";
        }
        return "401";
    }
}
