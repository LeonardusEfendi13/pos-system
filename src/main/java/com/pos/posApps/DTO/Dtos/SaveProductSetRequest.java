package com.pos.posApps.DTO.Dtos;

import lombok.Data;

import java.util.List;

@Data
public class SaveProductSetRequest {
    private Long productSetId;
    private Long parentProductId;
    private List<SaveProductSetComponentRequest> components;
}
