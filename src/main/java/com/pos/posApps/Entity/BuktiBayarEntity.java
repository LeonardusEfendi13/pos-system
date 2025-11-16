package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "bukti_bayar")
public class BuktiBayarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bukti_bayar_seq")
    @SequenceGenerator(name = "bukti_bayar_seq", sequenceName = "bukti_bayar_sequences", allocationSize = 1)
    @Column(name = "bukti_bayar_id")
    private Long buktiBayarId;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "rekening_asal")
    private String rekeningAsal;

    @Column(name = "rekening_tujuan")
    private String rekeningTujuan;

    @Column(name = "jenis_bayar")
    private String jenisBayar;

    @ManyToOne
    @JoinColumn(name = "pembelian_id")
    private PurchasingEntity purchasingEntity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}

