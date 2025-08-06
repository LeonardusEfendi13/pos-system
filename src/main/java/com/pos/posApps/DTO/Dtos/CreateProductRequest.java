package com.pos.posApps.DTO.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProductRequest {
    //todo

    @NotBlank
    private String fullName;

    @NotBlank
    private String shortName;

    @NotBlank
    private String productId;

}
