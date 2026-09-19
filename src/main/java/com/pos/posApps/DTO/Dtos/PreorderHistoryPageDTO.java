package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreorderHistoryPageDTO {
    private List<PreorderHistoryRowDTO> content;
    private long totalElements;
    private int page;
    private int size;
    private List<SupplierLookupDTO> suppliers;
    private String role;
    private String accountName;
    private String startDate;
    private String endDate;
    private Long supplierId;
}
