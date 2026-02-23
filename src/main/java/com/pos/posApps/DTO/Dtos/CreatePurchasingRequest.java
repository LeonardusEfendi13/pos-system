package com.pos.posApps.DTO.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePurchasingRequest {
    private String purchasingNumber;
    private Long supplierId;
    private List<PurchasingDetailDTO> pembelianDetailDTOS;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;
    private BigDecimal totalDisc;
    private String poDate;
    private String poDueDate;
    @JsonProperty("isCash")
    private boolean isCash;
    @JsonProperty("isPaid")
    private boolean isPaid;

    @JsonProperty("isConverting")
    private boolean isConverting;

    private Long preorderId;

}
