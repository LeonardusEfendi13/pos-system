package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayrollHistoryDTO {
    private Long payrollHistoryId;
    private Integer month;
    private Integer year;
    private String staffName;
    private BigDecimal totalPaid;
}
