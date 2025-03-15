package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

//gambar = Sales
@Entity
@Data
public class TransactionEntity {

    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "transaction_number")
    private String transactionNumber;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private AccountEntity accountEntity;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerEntity customerEntity;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;


}
