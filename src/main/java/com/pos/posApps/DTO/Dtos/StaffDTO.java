package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffDTO {
    private Long staffId;
    private String nama;
    private String nik;
    private String tempatLahir;
    private LocalDateTime tanggalLahir;
    private LocalDateTime tanggalJoin;
    private LocalDateTime tanggalResign;
    private String jabatan;
    private BigDecimal gaji;
    private String noHp;
    private String noHpDarurat;
    private String jenisKelamin;
    private String pendidikanTerakhir;
}
