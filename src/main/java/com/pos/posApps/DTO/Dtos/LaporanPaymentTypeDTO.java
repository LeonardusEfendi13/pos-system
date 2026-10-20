package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPaymentTypeDTO {
    private String typeName;
    private BigDecimal amount;
    private List<LaporanPaymentMethodAmountDTO> methods;
}
