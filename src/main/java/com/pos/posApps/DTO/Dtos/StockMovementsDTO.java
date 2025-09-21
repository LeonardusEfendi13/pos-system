package com.pos.posApps.DTO.Dtos;


import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockMovementsDTO {
    private Long stockMovementsId;
    private String referenceNo;
    private TipeKartuStok tipeKartuStok;
    private Long qtyIn;
    private Long qtyOut;
    private Long saldo;
    private LocalDateTime createdAt;
}
