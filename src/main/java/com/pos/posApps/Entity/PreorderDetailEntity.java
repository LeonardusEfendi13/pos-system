package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "preorder_detail")
public class PreorderDetailEntity {
    @Id
    @Column(name = "preorder_detail_id")
    private String preorderDetailId;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name= "stock")
    private Long stock;

    @Column(name = "supplier_price")
    private BigDecimal supplierPrice;
}
