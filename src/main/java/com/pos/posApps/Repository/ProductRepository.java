package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductEntity;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Page<ProductEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullAndFullNameContainingIgnoreCaseOrderByProductIdDesc(
            Long clientId,
            String search,
  
    @Query("SELECT p FROM ProductEntity p " +
            "WHERE p.clientEntity.clientId = :clientId " +
            "AND p.deletedAt IS NULL " +
            "AND (" +
            "    LOWER(p.shortName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%'))" +
            ") " +
            "ORDER BY p.productId DESC")
    Page<ProductEntity> searchProducts(
            @Param("clientId") Long clientId,
            @Param("search") String search,
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

    @Query("""
                SELECT p.productId
                FROM ProductEntity p
                WHERE p.clientEntity.clientId = :clientId
                  AND p.deletedAt IS NULL
                  AND p.productPricesEntity IS NOT EMPTY
                ORDER BY p.productId DESC
            """)
    Page<Long> findProductIds(@Param("clientId") Long clientId, Pageable pageable);


    @Query("""
                SELECT p FROM ProductEntity p
                WHERE p.clientEntity.clientId = :clientId
                  AND p.productPricesEntity IS NOT EMPTY
                  AND p.deletedAt IS NULL
                ORDER BY p.productId DESC
            """)
    @QueryHints(value = {
            @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "1000")
    })
    Stream<ProductEntity> streamAllByClientId(@Param("clientId") long clientId);
}
