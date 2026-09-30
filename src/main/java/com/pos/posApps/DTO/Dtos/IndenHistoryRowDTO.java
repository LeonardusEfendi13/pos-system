package com.pos.posApps.DTO.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndenHistoryRowDTO {
    private Long id;
    private String indenNumber;
    private LocalDateTime tanggalInden;
    private String custName;
    private String custPhone;
    private BigDecimal totalPrice;
    private BigDecimal deposit;
    private BigDecimal sisaBayar;
    private String createdBy;
    private String statusInden;
    @JsonProperty("isCash")
    private boolean isCash = true;
    @JsonProperty("isPaid")
    private boolean isPaid = true;
    private BigDecimal paidAmount;
    private LocalDateTime dueDate;
    private Long paymentMethodId;
    private String paymentMethodName;
    private String paymentMethodType;
    private String paymentMethodRekening;
}
