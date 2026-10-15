package com.pos.posApps.Entity;

import com.pos.posApps.DTO.Enum.Jabatan;
import com.pos.posApps.DTO.Enum.JenisKelamin;
import com.pos.posApps.DTO.Enum.Pendidikan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @Column(name = "nama")
    private String nama;

    @Column(name = "nik")
    private String nik;

    @Column(name = "tempat_lahir")
    private String tempatLahir;

    @Column(name = "tanggal_lahir")
    private LocalDate tanggalLahir;

    @Column(name = "tanggal_join")
    private LocalDate tanggalJoin;

    @Column(name = "tanggal_resign")
    private LocalDate tanggalResign;

    @Enumerated(EnumType.STRING)
    @Column(name = "jabatan")
    private Jabatan jabatan;

    @Column(name = "gaji_pokok")
    private BigDecimal gajiPokok;

    @Column(name = "uang_makan")
    private BigDecimal uangMakan;

    @Column(name = "insentif")
    private BigDecimal insentif;

    @Column(name = "no_hp")
    private String noHp;

    @Column(name = "no_hp_darurat")
    private String noHpDarurat;

    @Enumerated(EnumType.STRING)
    @Column(name = "jenis_kelamin")
    private JenisKelamin jenisKelamin;

    @Enumerated(EnumType.STRING)
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
