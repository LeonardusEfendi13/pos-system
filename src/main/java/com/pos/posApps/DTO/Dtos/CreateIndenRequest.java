package com.pos.posApps.DTO.Dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateIndenRequest {
    private List<IndenDetailDTO> indenDetailDTOS;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;
    private BigDecimal totalDisc;
    private String customerName;
    private String customerPhone;
    private BigDecimal deposit;
}
