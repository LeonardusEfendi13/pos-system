package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreorderHistoryRowDTO {
    private Long preorderId;
    private LocalDateTime createdAt;
    private BigDecimal totalPrice;
    private Long supplierId;
    private String supplierName;
}
