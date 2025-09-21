package com.pos.posApps.DTO.Dtos;

import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdjustStockDTO {
    private ProductEntity productEntity;
    private String referenceNo;
    private TipeKartuStok tipeKartuStok;
    private Long qtyIn;
    private Long qtyOut;
    private Long saldo;
    private ClientEntity clientData;
}
