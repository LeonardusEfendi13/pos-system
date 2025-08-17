package com.pos.posApps.Controller;

import com.pos.posApps.DTO.Dtos.ProductDTO;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.CustomerService;
import com.pos.posApps.Service.KasirService;
import com.pos.posApps.Service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("kasir")
@AllArgsConstructor
public class KasirController {
    private AuthService authService;
    private KasirService kasirService;
    private CustomerService customerService;
    private ProductService productService;

    @GetMapping
    public String displayKasir(Model model, HttpSession session){
        String clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }
        List<ProductDTO> productEntity = productService.getProductData(clientId);
        List<CustomerEntity> customerEntities = customerService.getCustomerList(clientId);
        System.out.println("product entity : " + productEntity);
        System.out.println("customer entity : " + customerEntities);

        model.addAttribute("activePage", "kasir");
        model.addAttribute("productData", productEntity);
        model.addAttribute("customerData", customerEntities);
        return "display_kasir";
    }
}
