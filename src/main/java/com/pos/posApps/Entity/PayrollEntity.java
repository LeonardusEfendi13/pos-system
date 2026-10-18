package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private StaffEntity staffEntity;

    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "payroll_month")
    private int payrollMonth;

    @Column(name = "payroll_year")
    private int payrollYear;

    @Column(name = "days_in_month")
    private int daysInMonth;

    @Column(name = "absence_count")
    private int absenceCount;

    @Column(name = "deducted_days")
    private int deductedDays;

    @Column(name = "daily_rate")
    private BigDecimal dailyRate;

    @Column(name = "gaji_pokok")
    private BigDecimal gajiPokok;

    @Column(name = "uang_makan")
    private BigDecimal uangMakan;

    @Column(name = "insentif")
    private BigDecimal insentif;

    @Column(name = "potongan")
    private BigDecimal potongan;

    @Column(name = "gaji_pokok_dibayar")
    private BigDecimal gajiPokokDibayar;

    @Column(name = "total_payroll")
    private BigDecimal totalPayroll;

    // Null until the month's salary is marked paid; the row keeps a snapshot
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
