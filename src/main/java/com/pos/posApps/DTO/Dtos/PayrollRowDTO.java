package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRowDTO {
    private Long staffId;
    private String nama;
    private String jabatan;
    private BigDecimal gajiPokok;
    private BigDecimal uangMakan;
    private BigDecimal insentif;
    private int daysInMonth;
    private int absenceCount;
    private int deductedDays;
    private BigDecimal dailyRate;
    private BigDecimal potongan;
    private BigDecimal gajiPokokDibayar;
    private BigDecimal total;
}
