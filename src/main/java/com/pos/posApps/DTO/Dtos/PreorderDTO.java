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
public class PreorderDTO {
    private Long preorderId;
    private SupplierDTO supplierDTO;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;
    private BigDecimal totalDisc;
    private LocalDateTime createdAt;
    private List<PreorderDetailDTO> preorderDetailDTOS;
}
