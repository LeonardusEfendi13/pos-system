package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreorderHistoryDetailDTO {
    private Long preorderId;
    private LocalDateTime createdAt;
    private String supplierName;
    private BigDecimal subtotal;
    private BigDecimal totalDisc;
    private BigDecimal totalPrice;
    private List<PreorderHistoryLineDTO> lines;
}
