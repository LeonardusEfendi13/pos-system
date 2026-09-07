package com.pos.posApps.DTO.Dtos.Home;

import com.pos.posApps.DTO.Dtos.SidebarDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardApiResponse {
    private HomeTopBarDTO topBarData;
    private SidebarDTO sidebarData;
    private ChartDTO chartDatas;
    private List<HomeProductDTO> homeProductData;
    private List<DashboardHomeCustomerDTO> homeCustomerData;
    private String startDate;
    private String endDate;
    private String periodFilter;
}
