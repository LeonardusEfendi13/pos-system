package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceMonthResponse {
    private int month;
    private int year;
    private int daysInMonth;
    private String role;
    private String accountName;
    private List<AttendanceRowDTO> rows;
}
