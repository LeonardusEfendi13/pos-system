package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

//Gambar = sold_product
@Entity
@Data
public class TransactionDetailEntity {

    @Id
    @Column(name = "transaction_detail_id")
    private String transactionProductId;

    @Column(name = "total_product")
    private Long totalProduct;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;

    @Column(name = "discount")
    private BigDecimal discount;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
