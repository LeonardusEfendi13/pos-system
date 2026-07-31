package com.pos.posApps.DTO.Dtos;

import lombok.Data;

import java.util.List;

@Data
public class AttendanceBatchForm {
    private List<AttendanceItem> attendances;

    @Data
    public static class AttendanceItem {
        private Long staffId;
        private String status; // HADIR, SAKIT, IZIN, ALFA
    }
}
