package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "vehicle")
public class VehicleEntity {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "brand")
    private String brand;

    @Column(name = "model")
    private String model;

    @Column(name = "known_part_number")
    private String knownPartNumber;

    @OneToMany(mappedBy = "vehicleEntity")
    private List<CompatibleProductsEntity> compatibleProductsEntities;
}
