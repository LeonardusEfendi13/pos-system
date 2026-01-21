package com.pos.posApps.Repository;

import com.pos.posApps.Entity.CompatibleProductsEntity;
import com.pos.posApps.Entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompatibleProductsRepository extends JpaRepository<CompatibleProductsEntity, Long> {
}
