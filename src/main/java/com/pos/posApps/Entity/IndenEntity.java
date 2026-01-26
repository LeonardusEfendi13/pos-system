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

    @Column(name = "is_ordered")
    private Boolean isOrdered;

    @Column(name = "is_available")
    private Boolean isAvailable;

    @Column(name = "is_delivered")
    private Boolean isDelivered;

    @OneToMany(mappedBy = "indenEntity")
    private List<IndenDetailEntity> indenDetailEntities;
}

