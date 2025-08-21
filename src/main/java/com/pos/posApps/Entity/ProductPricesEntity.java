package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "product_prices")
public class ProductPricesEntity {

    @Id
    @Column(name = "product_prices_id")
    private Long productPricesId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;

    @Column(name = "percentage")
    private BigDecimal percentage;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "maximal_count")
    private Long maximalCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
