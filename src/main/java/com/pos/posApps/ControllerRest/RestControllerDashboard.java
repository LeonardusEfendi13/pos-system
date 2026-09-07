package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.Home.ChartDTO;
import com.pos.posApps.DTO.Dtos.Home.DashboardApiResponse;
import com.pos.posApps.DTO.Dtos.Home.DashboardHomeCustomerDTO;
import com.pos.posApps.DTO.Dtos.Home.DashboardHomeSupplierDTO;
import com.pos.posApps.DTO.Dtos.Home.HomeProductDTO;
import com.pos.posApps.DTO.Dtos.Home.HomeTopBarDTO;
import com.pos.posApps.DTO.Dtos.SidebarDTO;
import com.pos.posApps.DTO.Dtos.UserDTO;
import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Service.AccountService;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.DashboardCustomerService;
import com.pos.posApps.Service.DashboardSupplierService;
import com.pos.posApps.Service.HomeService;
import com.pos.posApps.Service.SidebarService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("v3/dashboard")
@AllArgsConstructor
public class RestControllerDashboard {

    private AuthService authService;
    private HomeService homeService;
    private DashboardCustomerService dashboardCustomerService;
    private DashboardSupplierService dashboardSupplierService;
    private SidebarService sidebarService;
    private AccountService accountService;

    @GetMapping
    public ResponseEntity<DashboardApiResponse> getDashboard(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String periodFilter) {
        String token;
        Long clientId;

        try {
            token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        UserDTO userData = accountService.getCurrentLoggedInUser(token);
        if (!userData.getRole().equals(Roles.SUPER_ADMIN)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }

        periodFilter = (periodFilter == null || periodFilter.isBlank()) ? "day" : periodFilter;
        startDate = (startDate == null || startDate.isBlank())
                ? LocalDate.now().minusDays(7).toString()
                : startDate;
        endDate = (endDate == null || endDate.isBlank())
                ? LocalDate.now().toString()
                : endDate;

        LocalDateTime inputStartDate = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime inputEndDate = LocalDate.parse(endDate).atTime(23, 59, 59);

        LocalDateTime finalStartDate = homeService.adjustStartDate(inputStartDate, periodFilter);
        LocalDateTime finalEndDate = homeService.adjustEndDate(inputEndDate, periodFilter);

        HomeTopBarDTO topBarData = homeService.getHomeTopBarData(clientId);
        List<HomeProductDTO> homeProductData = homeService.getTop10Product(finalStartDate, finalEndDate);
        List<DashboardHomeCustomerDTO> homeCustomerData =
                dashboardCustomerService.getTop10CustomerWithProfit(clientId, finalStartDate, finalEndDate);
        List<DashboardHomeSupplierDTO> homeSupplierData =
                dashboardSupplierService.getTopSuppliers(clientId, finalStartDate, finalEndDate);
        ChartDTO chartDatas = homeService.getChartData(clientId, finalStartDate, finalEndDate, periodFilter);
        SidebarDTO sidebarData = sidebarService.getSidebarData(clientId, token);

        DashboardApiResponse response = new DashboardApiResponse(
                topBarData,
                sidebarData,
                chartDatas,
                homeProductData,
                homeCustomerData,
                homeSupplierData,
                finalStartDate.toLocalDate().toString(),
                finalEndDate.toLocalDate().toString(),
                periodFilter
        );

        return ResponseEntity.ok(response);
    }
}
