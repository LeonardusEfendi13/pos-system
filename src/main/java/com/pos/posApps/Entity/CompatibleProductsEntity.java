package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "compatible_products")
public class CompatibleProductsEntity {
    @Id
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicleEntity;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;

    @Column(name = "year_start")
    private String yearStart;

    @Column(name = "year_end")
    private String yearEnd;

    @Column(name = "is_valid")
    private Boolean isValid;
}
