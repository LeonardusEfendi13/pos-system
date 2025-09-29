package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.Home.HomeCustomerDTO;
import com.pos.posApps.DTO.Dtos.Home.HomeProductDTO;
import com.pos.posApps.DTO.Dtos.Home.HomeTopBarDTO;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.HomeService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Controller
@RequestMapping("home")
@AllArgsConstructor
public class HomeController {

    private AuthService authService;
    private HomeService homeService;

    @GetMapping
    public String home(HttpSession session, Model model, String startDate, String endDate){
        Long clientId;
        try{
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        }catch (Exception e){
            return "redirect:/login";
        }

        startDate = (startDate == null || startDate.isBlank()) ? LocalDate.now().minusDays(7).toString() : startDate;
        endDate = (endDate == null || endDate.isBlank())? LocalDate.now().toString() : endDate;


        LocalDateTime inputStartDate = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime inputEndDate = LocalDate.parse(endDate).atTime(23, 59, 59);


        HomeTopBarDTO topBarData = homeService.getHomeTopBarData(clientId);
        List<HomeProductDTO> homeProductData = homeService.getTop10Product(inputStartDate, inputEndDate);
        List<HomeCustomerDTO> homeCustomerData = homeService.getTop5Customer(clientId, inputStartDate, inputEndDate);
        System.out.println("Home customer data : " + homeCustomerData);
        model.addAttribute("topBarData", topBarData);
        model.addAttribute("homeCustomerData", homeCustomerData);
        model.addAttribute("homeProductData", homeProductData);
        model.addAttribute("activePage", "home");
        return "home";
    }
}
