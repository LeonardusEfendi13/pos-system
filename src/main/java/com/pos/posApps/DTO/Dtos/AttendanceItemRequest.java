package com.pos.posApps.DTO.Dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceItemRequest {
    private Long staffId;
    private LocalDate date;
    private String status;
}
