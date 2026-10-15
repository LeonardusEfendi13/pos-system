package com.pos.posApps.DTO.Dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StaffWriteRequest {
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
