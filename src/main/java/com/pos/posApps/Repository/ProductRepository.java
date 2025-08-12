package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {

    List<ProductEntity> findAllByClientEntity_ClientIdOrderByProductIdAsc(String clientId);

    ProductEntity findFirstByFullNameOrShortNameOrProductId(String fullName, String shortName, String productId);

    ProductEntity findFirstByOrderByProductIdDesc();

    ProductEntity findFirstByProductIdAndDeletedAtIsNull(String productId);
}
