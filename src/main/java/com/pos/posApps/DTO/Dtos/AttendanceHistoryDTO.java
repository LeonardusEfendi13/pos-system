package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceHistoryDTO {
    private Long staffId;
    private String namaStaff;
    private Map<Integer, String> statusPerTanggal;
    private Long totalHadir;
    private Long totalSakit;
    private Long totalIzin;
    private Long totalAlfa;
}
