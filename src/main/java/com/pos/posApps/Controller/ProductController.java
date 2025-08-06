package com.pos.posApps.Controller;

import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("api/v1/products")
@AllArgsConstructor
public class ProductController {
    private AuthService authService;
    private ProductService productService;
}
