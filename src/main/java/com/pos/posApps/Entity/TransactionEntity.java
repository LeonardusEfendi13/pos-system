package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

//gambar = Sales
@Entity
@Data
@Table(name = "transaction")
public class TransactionEntity {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @Column(name = "transaction_number")
    private String transactionNumber;

//    @ManyToOne
//    @JoinColumn(name = "account_id")
//    private AccountEntity accountEntity;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerEntity customerEntity;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "transactionEntity")
    private List<TransactionDetailEntity> transactionDetailEntities;
}
