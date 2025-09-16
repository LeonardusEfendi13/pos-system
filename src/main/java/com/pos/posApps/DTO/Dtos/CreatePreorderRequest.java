package com.pos.posApps.DTO.Dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePreorderRequest {
    private Long supplierId;
    private List<PreorderDetailDTO> preorderDetailDTOS;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;
    private BigDecimal totalDisc;
}
