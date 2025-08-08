package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "preorder")
public class PreorderEntity {
    @Id
    @Column(name = "preorder_id")
    private String preorderId;

    @Column(name = "preorder_detail_id")
    private String preorderDetailId;

    @Column(name = "supplier_id")
    private String supplierId;
}
