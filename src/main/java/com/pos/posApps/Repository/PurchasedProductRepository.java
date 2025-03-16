package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PurchasedProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchasedProductRepository extends JpaRepository<PurchasedProductEntity, String> {
}
