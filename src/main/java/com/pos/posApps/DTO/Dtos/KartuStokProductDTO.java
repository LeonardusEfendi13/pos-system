package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KartuStokProductDTO {
    private Long productId;
    private String shortName;
    private String fullName;
    private Long stok;
}
