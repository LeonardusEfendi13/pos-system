package com.pos.posApps.DTO.Dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private Long productId;
    private String shortName;
    private String fullName;
    private BigDecimal hargaBeli;
    private Long stok;
    private List<ProductPricesDTO> productPricesDTO;
    private Long supplierId;
    private Long minimumStock;
}
