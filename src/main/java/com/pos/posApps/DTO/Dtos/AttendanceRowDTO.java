package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRowDTO {
    private Long staffId;
    private String nama;
    private String jabatan;
    private LocalDate tanggalJoin;
    private LocalDate tanggalResign;
    private int absenceCount;
    private int remainingAllowance;
    private int deductedDays;
    private Map<Integer, String> statuses;
}
