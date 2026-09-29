package com.pos.posApps.DTO.Dtos;

import lombok.Data;

import java.util.List;

@Data
public class PretelDraftDTO {
    private Long parentProductId;
    private Integer clickCount;
    private List<PretelDraftLineDTO> lines;
}
