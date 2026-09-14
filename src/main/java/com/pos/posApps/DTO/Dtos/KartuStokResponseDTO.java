package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KartuStokResponseDTO {
    private String role;
    private String accountName;
    private KartuStokProductDTO product;
    private Long stockAwal;
    private Long qtyInTotal;
    private Long qtyOutTotal;
    private Long saldoAkhir;
    private List<StockMovementsDTO> movements;
}
