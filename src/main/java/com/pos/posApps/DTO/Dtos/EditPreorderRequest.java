package com.pos.posApps.DTO.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class EditPreorderRequest {
    @NotBlank(message = "Preorder id must not null")
    private String preorderId;

    @NotBlank(message = "supplier id must not null")
    private String supplierId;

    @NotBlank(message = "Client id must not null")
    private String clientId;

    private List<PreorderDetailDTO> preorderDetailData;

}
