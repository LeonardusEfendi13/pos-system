package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KasirClientDTO {
    private String name;
    private String alamat;
    private String kota;
    private String noTelp;
    private String catatan;
}
