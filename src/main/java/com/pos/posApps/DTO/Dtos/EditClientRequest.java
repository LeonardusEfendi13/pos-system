package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class EditClientRequest {
    private String name;
    private Long clientId;
    private String alamat;
    private String kota;
    private String noTelp;
    private String catatan;
}
