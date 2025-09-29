package com.pos.posApps.DTO.Dtos.Home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GraphicDTO {
    private List<String> labels;
    private List<BigDecimal> pendapatan;
    private List<BigDecimal> pengeluaran;
    private List<BigDecimal> laba;
}
