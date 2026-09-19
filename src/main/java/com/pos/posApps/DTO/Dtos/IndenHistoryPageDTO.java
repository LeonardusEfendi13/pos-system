package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndenHistoryPageDTO {
    private List<IndenHistoryRowDTO> content;
    private long totalElements;
    private int page;
    private int size;
    private String role;
    private String accountName;
    private String startDate;
    private String endDate;
    private String statusInden;
}
