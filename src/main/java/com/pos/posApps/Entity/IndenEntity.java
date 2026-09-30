package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "inden")
public class IndenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inden_seq")
    @SequenceGenerator(name = "inden_seq", sequenceName = "inden_sequences", allocationSize = 1)
    @Column(name = "id")
    private Long indenId;

    @Column(name = "inden_number")
    private String indenNumber;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToOne
    @JoinColumn(name = "created_by")
    private AccountEntity accountEntity;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "deposit")
    private BigDecimal deposit;

    @Column(name = "is_cash")
    private boolean isCash = true;

    @Column(name = "is_paid")
    private boolean isPaid = true;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "payment_method_name")
    private String paymentMethodName;

    @Column(name = "payment_method_type")
    private String paymentMethodType;

    @Column(name = "payment_method_rekening")
    private String paymentMethodRekening;

    @Column(name = "bukti_original_name")
    private String buktiOriginalName;

    @Column(name = "bukti_file_path")
    private String buktiFilePath;

    @Column(name = "status_inden")
    private String statusInden;

    @OneToMany(mappedBy = "indenEntity")
    private List<IndenDetailEntity> indenDetailEntities;
}

