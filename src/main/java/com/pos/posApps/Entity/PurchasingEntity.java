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
@Table(name = "purchasing")
public class PurchasingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchasing_seq")
    @SequenceGenerator(name = "purchasing_seq", sequenceName = "purchasing_sequences", allocationSize = 1)
    @Column(name = "purchasing_id")
    private Long purchasingId;

    @Column(name = "purchasing_number")
    private String purchasingNumber;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private SupplierEntity supplierEntity;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "po_date")
    private LocalDateTime poDate;

    @Column(name = "po_due_date")
    private LocalDateTime poDueDate;

    @Column(name = "is_cash")
    private boolean isCash;

    @Column(name = "is_paid")
    private boolean isPaid;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @OneToMany(mappedBy = "purchasingEntity")
    private List<PurchasingDetailEntity> purchasingDetailEntities;

    @OneToOne
    @JoinColumn(name = "created_by")
    private AccountEntity accountEntity;
}
