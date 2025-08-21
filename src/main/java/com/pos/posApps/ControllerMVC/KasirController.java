package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.CreateTransactionRequest;
import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.DTO.Dtos.ProductDTO;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("kasir")
@AllArgsConstructor
public class KasirController {
    private AuthService authService;
    private CustomerService customerService;
    private ProductService productService;
    private PenjualanService penjualanService;

    @GetMapping
    public String displayKasir(Model model, HttpSession session, Long transactionId){
        System.out.println("display called");
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }
        List<ProductDTO> productEntity = productService.getProductData(clientId);
        List<CustomerEntity> customerEntities = customerService.getCustomerList(clientId);

        PenjualanDTO penjualanData = new PenjualanDTO();
        if(transactionId != null){
            penjualanData = penjualanService.getPenjualanDataById(clientId, transactionId);
        }
        model.addAttribute("penjualanData", penjualanData);
        model.addAttribute("activePage", "kasir");
        model.addAttribute("productData", productEntity);
        model.addAttribute("customerData", customerEntities);
        System.out.println("customer data : " + customerEntities);
        return "display_kasir";
    }

    @PostMapping("/add")
    public String addKasir(@Valid @RequestBody CreateTransactionRequest req, Model model, HttpSession session){
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }
        System.out.println("request received : " + req);
        return "display_kasir";
    }
}
