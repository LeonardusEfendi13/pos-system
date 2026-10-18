package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollMonthResponse {
    private int month;
    private int year;
    // Date the running month is calculated up to
    private LocalDate asOf;
    private String role;
    private String accountName;
    private List<PayrollRowDTO> rows;
}
