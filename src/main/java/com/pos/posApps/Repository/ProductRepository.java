package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findAllByClientEntity_ClientIdAndProductPricesEntityIsNotNullAndDeletedAtIsNullOrderByProductIdDesc(Long clientId);

    ProductEntity findFirstByFullNameOrShortNameOrProductIdAndClientEntity_ClientIdAndDeletedAtIsNull(String fullName, String shortName, Long productId, Long clientId);

    ProductEntity findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(String fullname, String shortName, Long clientId);

    Optional<ProductEntity> findFirstByProductIdAndDeletedAtIsNull(Long productId);

    boolean existsByFullNameAndClientEntity_ClientIdAndDeletedAtIsNullAndProductIdNot(
            String fullName, Long clientId, Long excludedProductId
    );

    boolean existsByShortNameAndClientEntity_ClientIdAndDeletedAtIsNullAndProductIdNot(
            String shortName, Long clientId, Long excludedProductId
    );

    boolean existsByProductIdAndClientEntity_ClientIdAndDeletedAtIsNullAndProductIdNot(
            Long productId, Long clientId, Long excludedProductId
    );}
