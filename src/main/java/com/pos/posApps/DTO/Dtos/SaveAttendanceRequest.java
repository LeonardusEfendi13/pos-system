package com.pos.posApps.DTO.Dtos;

import lombok.Data;

import java.util.List;

@Data
public class SaveAttendanceRequest {
    private List<AttendanceItemRequest> items;
}
