package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSetDTO {
    private Long productSetId;
    private Long parentProductId;
    private String parentShortName;
    private String parentFullName;
    private Long parentStock;
    private List<ProductSetComponentDTO> components;
}
