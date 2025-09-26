package com.pos.posApps.DTO.Dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanNilaiPersediaanDTO {
    private String shortName;
    private String fullName;
    private Long qty;
    private BigDecimal supplierPrice;
    private BigDecimal hargaJual;
    private BigDecimal totalPrice;
}
