package com.pos.posApps.Entity;

import com.pos.posApps.DTO.Enum.Jabatan;
import com.pos.posApps.DTO.Enum.JenisKelamin;
import com.pos.posApps.DTO.Enum.JenisKomponenGaji;
import com.pos.posApps.DTO.Enum.Pendidikan;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "komponen_gaji")
public class KomponenGajiEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "komponen_gaji_seq")
    @SequenceGenerator(name = "komponen_gaji_seq", sequenceName = "komponen_gaji_sequences", allocationSize = 1)
    @Column(name = "komponen_gaji_id")
    private Long komponenGajiId;

    @Column(name = "nama_komponen")
    private String namaKomponen;

    @Enumerated(EnumType.STRING)
    @Column(name = "jenis")
    private JenisKomponenGaji jenis;

    @Column(name = "is_insentif")
    private boolean isInsentif;

    @Column(name = "is_active")
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}

