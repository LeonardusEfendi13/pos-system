package com.pos.posApps.DTO.Dtos;


import com.pos.posApps.DTO.Enum.EnumRole.StatusInden;
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
    private String createdBy;
    private String custName;
    private String custPhone;
    private StatusInden statusInden;
}
