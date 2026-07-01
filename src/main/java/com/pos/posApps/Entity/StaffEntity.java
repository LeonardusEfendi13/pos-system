package com.pos.posApps.Entity;

import com.pos.posApps.DTO.Enum.Jabatan;
import com.pos.posApps.DTO.Enum.JenisKelamin;
import com.pos.posApps.DTO.Enum.Pendidikan;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "staff")
public class StaffEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "staff_seq")
    @SequenceGenerator(name = "staff_seq", sequenceName = "staff_sequences", allocationSize = 1)
    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "nama")
    private String nama;

    @Column(name = "nik")
    private String nik;

    @Column(name = "tempat_lahir")
    private String tempatLahir;

    @Column(name = "tanggal_lahir")
    private LocalDateTime tanggalLahir;

    @Column(name = "tanggal_join")
    private LocalDateTime tanggalJoin;

    @Column(name = "tanggal_resign")
    private LocalDateTime tanggalResign;

    @Enumerated(EnumType.STRING)
    @Column(name = "jabatan")
    private Jabatan jabatan;

    @Column(name = "gaji")
    private BigDecimal gaji;

    @Column(name = "no_hp")
    private String noHp;

    @Column(name = "no_hp_darurat")
    private String noHpDarurat;

    @Column(name = "jenis_kelamin")
    private JenisKelamin jenisKelamin;

    @Column(name = "pendidikan_terakhir")
    private Pendidikan pendidikanTerakhir;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}

