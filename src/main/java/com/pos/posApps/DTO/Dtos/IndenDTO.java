package com.pos.posApps.DTO.Dtos;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndenDTO {
    private Long id;
    private String indenNumber;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;
    private BigDecimal totalDisc;
    private LocalDateTime tanggalInden;
    private List<IndenDetailDTO> indenDetailDTOS;
    private BigDecimal deposit;
    private BigDecimal sisaBayar;
    private String createdBy;
    private String custName;
    private String custPhone;
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
