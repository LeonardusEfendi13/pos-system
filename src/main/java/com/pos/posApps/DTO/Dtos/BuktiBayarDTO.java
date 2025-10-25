package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuktiBayarDTO {
    private Long buktiBayarId;
    private String originalName;
    private String filePath; // URL to preview
    private LocalDateTime tanggalBayar;
    private String jenisBayar;
    private String rekeningAsal;
    private String rekeningTujuan;
}
