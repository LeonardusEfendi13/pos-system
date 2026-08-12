package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayrollFormDTO {
    private Long employeeId;
    private Integer month;
    private Integer year;
    private List<PayrollDetailDTO> details = new ArrayList<>();
}
