package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class CreateStaffRequest {
    private String nik;
    private String nama;
    private String tempatLahir;
    private String tanggalLahir;
    private String noHp;
    private String noHpDarurat;
    private String jabatan;
    private String jenisKelamin;
    private String tanggalJoin;
    private String gaji;
    private String pendidikanTerakhir;
}
