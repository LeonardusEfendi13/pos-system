package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payroll_detail")
public class PayrollDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payroll_detail_seq")
    @SequenceGenerator(name = "payroll_detail_seq", sequenceName = "payroll_detail_sequences", allocationSize = 1)
    @Column(name = "payroll_detail_id")
    private Long payrollDetailId;

    @ManyToOne
    @JoinColumn(name = "payroll_id")
    private PayrollEntity payrollEntity;

    @ManyToOne
    @JoinColumn(name = "komponen_gaji_id")
    private KomponenGajiEntity komponenGajiEntity;

    @Column(name = "amount")
    private BigDecimal amount;
}

