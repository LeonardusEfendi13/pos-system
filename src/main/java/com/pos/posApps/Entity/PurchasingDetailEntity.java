package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "mark_up_1")
    private BigDecimal markup1;

    @Column(name = "harga_jual_1")
    private BigDecimal hargaJual1;

    @Column(name = "mark_up_2")
    private BigDecimal markup2;

    @Column(name = "harga_jual_2")
    private BigDecimal hargaJual2;

    @Column(name = "mark_up_3")
    private BigDecimal markup3;

    @Column(name = "harga_jual_3")
    private BigDecimal hargaJual3;

    @ManyToOne
    @JoinColumn(name = "purchasing_id")
    private PurchasingEntity purchasingEntity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
