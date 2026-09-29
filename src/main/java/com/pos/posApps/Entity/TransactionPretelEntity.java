package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "transaction_pretel")
public class TransactionPretelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_pretel_seq")
    @SequenceGenerator(
            name = "transaction_pretel_seq",
            sequenceName = "transaction_pretel_sequences",
            allocationSize = 1
    )
    @Column(name = "transaction_pretel_id")
    private Long transactionPretelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private TransactionEntity transactionEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_product_id")
    private ProductEntity parentProduct;

    @Column(name = "set_qty")
    private Long setQty;

    @OneToMany(mappedBy = "transactionPretel")
    private List<TransactionPretelComponentEntity> components = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
