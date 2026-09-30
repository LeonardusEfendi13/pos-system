package com.pos.posApps.DTO.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateTransactionRequest {
    private Long customerId;
    private List<TransactionDetailDTO> transactionDetailDTOS;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;
    private BigDecimal totalDisc;
    private List<PretelDraftDTO> pretelDrafts;
    @JsonProperty("isCash")
    private Boolean isCash;
    private BigDecimal paymentAmount;
    private String dueDate;
    private Long paymentMethodId;
}
