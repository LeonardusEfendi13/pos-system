package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.ClientDTO;
import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.DTO.Dtos.ProductDTO;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.*;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    private ClientService clientService;

    @GetMapping
    public String displayKasir(Model model, HttpSession session, Long transactionId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return "redirect:/login";
        }

        Page<ProductDTO> productEntity = productService.getProductData(clientId, PageRequest.of(page, size));
        List<CustomerEntity> customerEntities = customerService.getCustomerList(clientId);
        ClientDTO clientSettingData = clientService.getClientSettings(clientId);

        System.out.println("productEntity = " + productEntity);
        System.out.println("customerEntity: " + customerEntities);
        model.addAttribute("clientSettingData", clientSettingData);


        PenjualanDTO penjualanData = (transactionId != null)
                ? penjualanService.getPenjualanDataById(clientId, transactionId)
                : new PenjualanDTO();

        model.addAttribute("penjualanData", penjualanData);
        model.addAttribute("activePage", "kasir");
        model.addAttribute("productData", productEntity.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productEntity.getTotalPages());
        model.addAttribute("customerData", customerEntities);
        model.addAttribute("settingData", clientSettingData);

        return "display_kasir_penjualan";
    }
}

