package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductPricesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductPricesRepository extends JpaRepository<ProductPricesEntity, Long> {
    void deleteAllByProductEntity_ProductId(Long productId);

    List<ProductPricesEntity> findAllByProductEntity_ProductIdOrderByProductPricesIdDesc(Long productId);
    List<ProductPricesEntity> findAllByProductEntity_ProductIdOrderByProductPricesIdAsc(Long productId);

}
