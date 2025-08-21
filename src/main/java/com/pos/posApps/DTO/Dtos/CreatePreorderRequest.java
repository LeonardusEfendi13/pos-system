package com.pos.posApps.DTO.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreatePreorderRequest {
    @NotBlank(message = "supplier id must not null")
    private Long supplierId;

    @NotBlank(message = "Client id must not null")
    private Long clientId;

    private List<PreorderDetailDTO> preorderDetailData;

}
