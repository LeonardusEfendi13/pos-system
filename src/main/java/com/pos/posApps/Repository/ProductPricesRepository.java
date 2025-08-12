package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductPricesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPricesRepository extends JpaRepository<ProductPricesEntity, String> {
    ProductPricesEntity findFirstByOrderByProductPricesIdDesc();

    ProductPricesEntity findFirstByProductPricesId(String productPricesId);

    List<ProductPricesEntity> findAllByProductEntity_ProductIdOrderByProductPricesIdAsc(String productId);

}
