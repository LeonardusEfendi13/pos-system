package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KasirPretelDTO {
    private Long parentProductId;
    private String parentName;
    private Long setQty;
    private List<KasirPretelComponentDTO> components;
}
