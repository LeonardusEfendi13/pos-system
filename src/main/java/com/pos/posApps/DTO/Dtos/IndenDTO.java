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
public class IndenDTO {
    private Long id;
    private String indenNumber;
    private BigDecimal subtotal;
    private BigDecimal totalPrice;
    private BigDecimal totalDisc;
    private LocalDateTime tanggalInden;
    private List<IndenDetailDTO> indenDetailDTOS;
    private String createdBy;
    private String custName;
    private String custPhone;
    private Boolean isOrdered;
    private Boolean isAvailable;
    private Boolean isDelivered;
}
