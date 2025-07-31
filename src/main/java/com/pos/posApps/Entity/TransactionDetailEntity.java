package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//Gambar = sold_product
@Entity
@Data
@Table(name = "transaction_detail")
public class TransactionDetailEntity {

    @Id
    @Column(name = "transaction_detail_id")
    private String transactionDetailId;

    @Column(name = "total_product")
    private Long totalProduct;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;

    @Column(name = "discount")
    private BigDecimal discount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
