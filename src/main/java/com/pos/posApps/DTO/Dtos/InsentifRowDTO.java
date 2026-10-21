package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsentifRowDTO {
    private Long staffId;
    private String nama;
    private String jabatan;
    private boolean resigned;
    // Current monthly insentif on the staff record
    private BigDecimal insentifBulanan;
    private int monthCount;
    // First and last saved month, as yyyy-MM
    private String firstPeriod;
    private String lastPeriod;
    private BigDecimal saldo;
    private LocalDateTime lastPayoutAt;
    private BigDecimal lastPayoutAmount;
}
