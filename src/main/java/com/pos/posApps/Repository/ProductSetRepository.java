package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductSetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSetRepository extends JpaRepository<ProductSetEntity, Long> {

    @Query("""
            SELECT s FROM ProductSetEntity s
            WHERE s.clientEntity.clientId = :clientId
              AND s.deletedAt IS NULL
              AND (
                :search = ''
                OR LOWER(s.parentProduct.shortName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(s.parentProduct.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<ProductSetEntity> search(
            @Param("clientId") Long clientId,
            @Param("search") String search,
            Pageable pageable
    );

    Optional<ProductSetEntity> findFirstByProductSetIdAndClientEntity_ClientIdAndDeletedAtIsNull(
            Long productSetId,
            Long clientId
    );

    Optional<ProductSetEntity> findFirstByClientEntity_ClientIdAndParentProduct_ProductIdAndDeletedAtIsNull(
            Long clientId,
            Long parentProductId
    );

    boolean existsByClientEntity_ClientIdAndParentProduct_ProductIdAndDeletedAtIsNullAndProductSetIdNot(
            Long clientId,
            Long parentProductId,
            Long productSetId
    );

    @Query("""
            SELECT s.parentProduct.productId
            FROM ProductSetEntity s
            WHERE s.clientEntity.clientId = :clientId
              AND s.deletedAt IS NULL
            """)
    List<Long> findActiveParentProductIds(@Param("clientId") Long clientId);
}
