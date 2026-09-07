package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateProductRequest;
import com.pos.posApps.DTO.Dtos.EditProductRequest;
import com.pos.posApps.DTO.Dtos.ProductDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/product")
@AllArgsConstructor
public class RestControllerProduct {
    private AuthService authService;
    private ProductService productService;

    @GetMapping("/find")
    public ResponseEntity<?> findProduct(
            HttpSession session,
            @RequestParam String keyword,
            @RequestParam Boolean isPurchasing) {
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(java.util.Map.of("message", "Unauthorized access"));
        }
        try {
            ProductDTO data = productService.findProductByCode(
                    clientId, keyword.toUpperCase(), isPurchasing);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(NOT_FOUND)
                    .body(java.util.Map.of("message", "Barang tidak ditemukan"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            HttpSession session,
            @RequestParam String keyword,
            @RequestParam(required = false) String field) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            Long clientId = authService.validateToken(token).getClientEntity().getClientId();
            return ResponseEntity.ok(
                    productService.searchProductByKeyword(clientId, keyword, field));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body(java.util.List.of());
        }
    }

    @GetMapping
    public ResponseEntity<?> getProductCatalog(
            HttpSession session,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            Long clientId = authService.validateToken(token).getClientEntity().getClientId();
            int safePage = Math.max(page, 0);
            int safeSize = Math.min(Math.max(size, 1), 50);
            return ResponseEntity.ok(
                    productService
                            .getProductData(clientId, PageRequest.of(safePage, safeSize), null, false)
                            .getContent());
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body(java.util.List.of());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<ProductDTO>> getProductList(HttpSession session){
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
            return ResponseEntity.ok(productService.getProductData(clientId, PageRequest.of(0, 10), null, true).getContent());
        }catch (Exception e){
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<String> addProducts(HttpSession session, @RequestBody CreateProductRequest req, RedirectAttributes redirectAttributes) {
        AccountEntity accEntity;
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            clientData = accEntity.getClientEntity();
            if (clientData.getClientId() == null) {
                return ResponseEntity.status(UNAUTHORIZED).body("Harap login ulang");
            }
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body("Something went wrong");
        }
        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            ResponseInBoolean isInserted = productService.insertProducts(req, clientData);
            if (isInserted.isStatus()) {
                return ResponseEntity.ok(isInserted.getMessage());
            }
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(isInserted.getMessage());
        }
        return ResponseEntity.status(UNAUTHORIZED).body("Anda tidak memiliki akses untuk ini!");
    }

    @PostMapping("/edit")
    public ResponseEntity<String> editProducts(HttpSession session, @RequestBody EditProductRequest req, RedirectAttributes redirectAttributes, String search) {
        AccountEntity accEntity;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accEntity = authService.validateToken(token);
            if (accEntity.getClientEntity().getClientId() == null) {
                redirectAttributes.addFlashAttribute("status", true);
                redirectAttributes.addFlashAttribute("message", "Session Expired");
                return ResponseEntity.status(UNAUTHORIZED).body("Harap login ulang");
            }
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Harap login ulang");
        }
        String encodedSearch = UriUtils.encode(search != null ? search : "", StandardCharsets.UTF_8);

        if (authService.hasAccessToModifyData(accEntity.getRole())) {
            ResponseInBoolean isEdited = productService.editProducts(req, accEntity.getClientEntity());
            if(isEdited.isStatus()){
                return ResponseEntity.ok(isEdited.getMessage());
            }
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(isEdited.getMessage());
        }
        return ResponseEntity.status(UNAUTHORIZED).body("Anda tidak memiliki akses untuk ini!");
    }
}
