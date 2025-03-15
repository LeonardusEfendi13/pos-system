package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Data
public class PurchasedProductEntity {

    @Id
    @Column(name = "purchased_product_id")
    private String purchasedProductId;

    @ManyToOne
    @JoinColumn(name = "purchasing_id")
    private PurchasingEntity purchasingEntity;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;

    @ManyToOne
    @JoinColumn(name = "product_prices_id")
    private ProductPricesEntity productPricesEntity;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
