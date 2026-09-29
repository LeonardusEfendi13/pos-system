package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transaction_pretel_component")
public class TransactionPretelComponentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_pretel_component_seq")
    @SequenceGenerator(
            name = "transaction_pretel_component_seq",
            sequenceName = "transaction_pretel_component_sequences",
            allocationSize = 1
    )
    @Column(name = "transaction_pretel_component_id")
    private Long transactionPretelComponentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_pretel_id")
    private TransactionPretelEntity transactionPretel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_product_id")
    private ProductEntity childProduct;

    @Column(name = "qty_in")
    private Long qtyIn;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
