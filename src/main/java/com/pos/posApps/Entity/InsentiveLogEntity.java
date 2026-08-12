package com.pos.posApps.Entity;

import com.pos.posApps.DTO.Enum.JenisKomponenGaji;
import com.pos.posApps.DTO.Enum.JenisTransaksiInsentive;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "insentive_log")
public class InsentiveLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "insentive_log_seq")
    @SequenceGenerator(name = "insentive_log_seq", sequenceName = "insentive_log_sequences", allocationSize = 1)
    @Column(name = "insentive_log_id")
    private Long insentiveLogId;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private StaffEntity staffEntity;

    @ManyToOne
    @JoinColumn(name = "payroll_id")
    private PayrollEntity payrollEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "jenis_transaksi")
    private JenisTransaksiInsentive jenisTransaksiInsentive;

    @Column(name = "nominal")
    private BigDecimal nominal;

    @Column(name = "catatan")
    private String catatan;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}

