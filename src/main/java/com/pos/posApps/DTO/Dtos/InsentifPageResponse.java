package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsentifPageResponse {
    private String role;
    private String accountName;
    private List<InsentifRowDTO> rows;
}
