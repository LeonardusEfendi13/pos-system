package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "product_set_component")
public class ProductSetComponentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_set_component_seq")
    @SequenceGenerator(
            name = "product_set_component_seq",
            sequenceName = "product_set_component_sequences",
            allocationSize = 1
    )
    @Column(name = "product_set_component_id")
    private Long productSetComponentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_set_id")
    private ProductSetEntity productSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_product_id")
    private ProductEntity childProduct;

    @Column(name = "qty")
    private Long qty;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
