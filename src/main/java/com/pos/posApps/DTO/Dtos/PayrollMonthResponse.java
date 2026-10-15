package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollMonthResponse {
    private int month;
    private int year;
    private boolean saved;
    private String role;
    private String accountName;
    private List<PayrollRowDTO> rows;
}
