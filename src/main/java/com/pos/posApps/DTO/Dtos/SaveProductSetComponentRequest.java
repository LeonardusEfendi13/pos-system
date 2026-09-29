package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class SaveProductSetComponentRequest {
    private Long productId;
    private Long qty;
}
