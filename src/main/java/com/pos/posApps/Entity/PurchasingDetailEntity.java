package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "purchasing_detail")
public class PurchasingDetailEntity {

    @Id
    @Column(name = "purchasing_detail_id")
    private Long purchasingDetailId;

    @Column(name ="short_name")
    private String shortName;

    @Column(name ="full_name")
    private String fullName;

    @Column(name ="qty")
    private Long qty;

    @Column(name ="price")
    private BigDecimal price;

    @Column(name ="disc_amount")
    private BigDecimal discAmount;

    @Column(name ="total_price")
    private BigDecimal totalPrice;

    @ManyToOne
    @JoinColumn(name = "purchasing_id")
    private PurchasingEntity purchasingEntity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
