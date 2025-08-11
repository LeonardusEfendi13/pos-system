package com.pos.posApps.Controller;

import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.KasirService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("kasir")
@AllArgsConstructor
public class KasirController {
    private AuthService authService;
    private KasirService kasirService;

    @GetMapping
    public String displayKasir(Model model){
        model.addAttribute("activePage", "kasir");
        return "display_kasir";
    }
}
