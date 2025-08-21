package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {

    List<ProductEntity> findAllByClientEntity_ClientIdAndProductPricesEntityIsNotNullOrderByCreatedAtDesc(String clientId);

    ProductEntity findFirstByFullNameOrShortNameOrProductIdAndDeletedAtIsNullAndClientEntity_ClientId(String fullName, String shortName, String productId, String clientId);

    ProductEntity findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(String fullname, String shortName, String clientId);

    Optional<ProductEntity> findFirstByOrderByCreatedAtDesc();

    ProductEntity findFirstByProductIdAndDeletedAtIsNull(String productId);
}
