package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//Gambar = sold_product
@Entity
@Data
@Table(name = "inden_detail")
public class IndenDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inden_detail_seq")
    @SequenceGenerator(name = "inden_detail_seq", sequenceName = "inden_detail_sequences", allocationSize = 1)
    @Column(name = "inden_detail_id")
    private Long indenDetailId;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "qty")
    private Long qty;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "disc_amount")
    private BigDecimal discountAmount;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @ManyToOne
    @JoinColumn(name = "inden_id")
    private IndenEntity indenEntity;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "basic_price")
    private BigDecimal basicPrice;

    @Column(name = "total_profit")
    private BigDecimal totalProfit;
}
