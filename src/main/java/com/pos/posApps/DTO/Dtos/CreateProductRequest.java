package com.pos.posApps.DTO.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {

    private Long productId;

    @NotBlank
    private String shortName;

    @NotBlank
    private String fullName;

    @NotBlank
    private BigDecimal supplierPrice;

    @NotBlank
    private Long supplierId;

    @NotBlank
    private Long stock;

    @NotBlank
    private Long minimumStock;

    @NotBlank
    private List<ProductPricesDTO> productPricesDTO;

    private List<CompatibleProductsDTO> compatibleVehicles;

}
