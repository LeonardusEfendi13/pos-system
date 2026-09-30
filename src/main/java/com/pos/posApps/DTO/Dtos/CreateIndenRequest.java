package com.pos.posApps.DTO.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isCash")
    private Boolean isCash;
    private String dueDate;
    private Long paymentMethodId;
}
