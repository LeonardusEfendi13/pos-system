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
@Table(name = "preorder")
public class PreorderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "preorder_seq")
    @SequenceGenerator(name = "preorder_seq", sequenceName = "preorder_sequences", allocationSize = 1)
    @Column(name = "preorder_id")
    private Long preorderId;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private SupplierEntity supplierEntity;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "preorderEntity")
    private List<PreorderDetailEntity> preorderDetailEntities;

}
