package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class PayrollMonthRequest {
    private Long staffId;
    private Integer month;
    private Integer year;
}
