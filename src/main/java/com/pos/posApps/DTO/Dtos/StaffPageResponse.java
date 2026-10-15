package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffPageResponse {
    private List<StaffDTO> content;
    private long totalElements;
    private int number;
    private int size;
    private int totalPages;
    private String role;
    private String accountName;
    private long totalStaff;
    private long activeStaff;
    private long resignedStaff;
    private BigDecimal activePayroll;
}
