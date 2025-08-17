package com.pos.posApps.DTO.Dtos;


import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private String productId;
    private String shortName;
    private String fullName;
    private BigDecimal hargaBeli;
    private Long stok;
    private List<ProductPricesDTO> productPricesDTO;
    private String supplierId;
}
