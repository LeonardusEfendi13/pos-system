package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardKaryawanDTO {
    private Long totalKaryawan;
    private Long karyawanAktif;
    private Long totalResign;
    private BigDecimal payroll;
    private List<StaffDTO> listStaff;
}
