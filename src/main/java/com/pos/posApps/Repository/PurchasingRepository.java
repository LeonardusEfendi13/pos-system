package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PurchasingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchasingRepository extends JpaRepository<PurchasingEntity, String> {
}
