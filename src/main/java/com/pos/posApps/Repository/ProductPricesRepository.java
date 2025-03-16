package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductPricesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductPricesRepository extends JpaRepository<ProductPricesEntity, String> {
}
