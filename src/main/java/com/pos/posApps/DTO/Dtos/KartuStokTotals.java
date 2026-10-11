package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KartuStokTotals {
    private Long qtyIn;
    private Long qtyOut;
    private Long pretel;
}
