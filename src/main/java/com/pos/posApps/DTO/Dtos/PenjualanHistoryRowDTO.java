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
public class PenjualanHistoryRowDTO {
    private Long transactionId;
    private String transactionNumber;
    private LocalDateTime tanggalJual;
    private BigDecimal totalPrice;
    private Long customerId;
    private String customerName;
    private String accountName;
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
