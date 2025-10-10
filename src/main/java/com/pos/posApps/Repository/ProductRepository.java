package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Page<ProductEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullAndFullNameContainingIgnoreCaseOrderByProductIdDesc(
            Long clientId,
            String search,
            Pageable pageable
    );


    Page<ProductEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByProductIdDesc(
            Long clientId,
            Pageable pageable
    );

    Page<ProductEntity> findAllByClientEntity_ClientIdAndProductPricesEntityIsNotNullAndDeletedAtIsNullOrderByProductIdDesc(Long clientId, Pageable pageable);

    List<ProductEntity> findAllByClientEntity_ClientIdAndProductPricesEntityIsNotNullAndDeletedAtIsNullOrderByProductIdDesc(Long clientId);

    ProductEntity findFirstByFullNameOrShortNameOrProductIdAndClientEntity_ClientIdAndDeletedAtIsNull(String fullName, String shortName, Long productId, Long clientId);

    ProductEntity findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(String fullname, String shortName, Long clientId);

    Optional<ProductEntity> findFirstByOrderByProductIdDesc();

    Optional<ProductEntity> findFirstByProductIdAndDeletedAtIsNull(Long productId);

    boolean existsByFullNameAndClientEntity_ClientIdAndDeletedAtIsNullAndProductIdNot(
            String fullName, Long clientId, Long excludedProductId
    );

    boolean existsByShortNameAndClientEntity_ClientIdAndDeletedAtIsNullAndProductIdNot(
            String shortName, Long clientId, Long excludedProductId
    );

    boolean existsByProductIdAndClientEntity_ClientIdAndDeletedAtIsNullAndProductIdNot(
            Long productId, Long clientId, Long excludedProductId
    );
}
