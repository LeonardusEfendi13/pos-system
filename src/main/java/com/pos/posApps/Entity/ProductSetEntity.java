package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "product_set")
public class ProductSetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_set_seq")
    @SequenceGenerator(name = "product_set_seq", sequenceName = "product_set_sequences", allocationSize = 1)
    @Column(name = "product_set_id")
    private Long productSetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_product_id")
    private ProductEntity parentProduct;

    @OneToMany(mappedBy = "productSet")
    private List<ProductSetComponentEntity> components = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
