package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductPricesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPricesRepository extends JpaRepository<ProductPricesEntity, String> {
    Optional<ProductPricesEntity> findFirstByOrderByCreatedAtDesc();

    void deleteAllByProductEntity_ProductId(String productId);

    List<ProductPricesEntity> findAllByProductEntity_ProductIdOrderByCreatedAtDesc(String productId);

}
