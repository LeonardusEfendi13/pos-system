package com.pos.posApps.DTO.Dtos.Home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeProductDTO {
    private String name;
    private Long qty;
}
