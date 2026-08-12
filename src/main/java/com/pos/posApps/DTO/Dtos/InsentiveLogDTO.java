package com.pos.posApps.DTO.Dtos;

import com.pos.posApps.DTO.Enum.Jabatan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InsentiveLogDTO {
    private Long staffId;
    private String staffName;
    private Jabatan jabatan;
    private BigDecimal currentBalance;
}
