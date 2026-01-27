package com.pos.posApps.Repository;

import com.pos.posApps.Entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<VehicleEntity, Long> {

    @Query("SELECT v FROM VehicleEntity v WHERE v.brand = 'YAMAHA'")
    List<VehicleEntity> findYamahaVehicleList();

    @Query("SELECT v FROM VehicleEntity v WHERE v.brand = 'HONDA'")
    List<VehicleEntity> findHondaVehicleList();

    List<VehicleEntity> findAllByBrand(String brand);

    Optional<VehicleEntity> findFirstByModelIgnoreCaseAndBrandIgnoreCase(String model, String brand);

    VehicleEntity findFirstById(Long id);

    Boolean existsByModelIgnoreCaseAndBrandIgnoreCaseAndIdNot(String model, String brand, Long vehicleId);

}
