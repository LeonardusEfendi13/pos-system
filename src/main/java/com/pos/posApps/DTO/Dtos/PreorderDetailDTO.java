package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreorderDetailDTO {
    private String preorderDetailId;
    private Long quantity;
    private String productId;
}
