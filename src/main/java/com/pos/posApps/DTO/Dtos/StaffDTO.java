package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffDTO {
    private Long staffId;
    private String nama;
    private String nik;
    private String tempatLahir;
    private LocalDate tanggalLahir;
    private LocalDate tanggalJoin;
    private LocalDate tanggalResign;
    private String jabatan;
    private BigDecimal gajiPokok;
    private BigDecimal uangMakan;
    private BigDecimal insentif;
    private String noHp;
    private String noHpDarurat;
    private String jenisKelamin;
    private String pendidikanTerakhir;
}
