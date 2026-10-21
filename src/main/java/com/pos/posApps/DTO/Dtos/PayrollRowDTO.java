package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private BigDecimal totalGaji;
    private int daysInMonth;
    private int workedDays;
    private int presentCount;
    private int absenceCount;
    private int deductedDays;
    private BigDecimal dailyRate;
    private BigDecimal gajiTerkumpul;
    private BigDecimal potongan;
    private BigDecimal total;
    private LocalDateTime paidAt;
}
