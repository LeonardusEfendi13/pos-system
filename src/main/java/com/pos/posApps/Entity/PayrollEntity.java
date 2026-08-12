package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payroll")
public class PayrollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payroll_seq")
    @SequenceGenerator(name = "payroll_seq", sequenceName = "payroll_sequences", allocationSize = 1)
    @Column(name = "payroll_id")
    private Long payrollId;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private StaffEntity staffEntity;

    @Column(name = "payroll_month")
    private int payrollMonth;

    @Column(name = "payroll_year")
    private int payrollYear;

    @Column(name = "total_payroll")
    private BigDecimal totalPayroll;

    @Column(name = "total_insentive")
    private BigDecimal totalInsentive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}

