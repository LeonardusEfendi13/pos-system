package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateProductRequest;
import com.pos.posApps.DTO.Dtos.ProductDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/product")
@AllArgsConstructor
public class RestControllerProduct {
    private AuthService authService;
    private ProductService productService;

    @GetMapping("/list")
    public ResponseEntity<List<ProductDTO>> getProductList(HttpSession session){
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
            return ResponseEntity.ok(productService.getProductData(clientId));
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
            boolean isInserted = productService.insertProducts(req, clientData);
            if (isInserted) {
                return ResponseEntity.ok("Berhasil menyimpan data");
            }
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body("Gagal menyimpan data");
        }
        return ResponseEntity.status(UNAUTHORIZED).body("Anda tidak memiliki akses untuk ini!");
    }
}
