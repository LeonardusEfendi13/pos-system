package com.pos.posApps.DTO.Dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClientSettingsRequest {
    private String name;
    private String alamat;
    private String kota;
    private String noTelp;
    private String catatan;
    private BigDecimal kingDiscYmh;
    private BigDecimal kingDiscHnd;
}
