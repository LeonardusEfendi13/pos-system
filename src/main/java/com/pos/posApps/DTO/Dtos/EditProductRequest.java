package com.pos.posApps.DTO.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EditProductRequest {
    @NotBlank
    private String productId;

    @NotBlank
    private String shortName;

    @NotBlank
    private String fullName;

    @NotBlank
    private BigDecimal percentage;

    @NotBlank
    private BigDecimal supplierPrice;

    @NotBlank
    private String supplierId;

    @NotBlank
    private Long stock;

    private List<ProductPricesDTO> productPricesDTO;

}
