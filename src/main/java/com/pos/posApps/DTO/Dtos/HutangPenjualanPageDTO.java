package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HutangPenjualanPageDTO {
    private List<PenjualanHistoryRowDTO> content;
    private long totalElements;
    private int page;
    private int size;
    private BigDecimal totalKredit;
    private BigDecimal sudahDibayar;
    private BigDecimal sisaHutang;
    private List<CustomerLookupDTO> customers;
    private String role;
    private String accountName;
    private String startDate;
    private String endDate;
    private Long customerId;
    private Boolean lunas;
}
