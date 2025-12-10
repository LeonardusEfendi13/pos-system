package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findAllByDeletedAtIsNull();

    @Query("SELECT p FROM ProductEntity p " +
            "WHERE p.clientEntity.clientId = :clientId " +
            "AND p.deletedAt IS NULL " +
            "AND (:supplierId IS NULL OR p.supplierEntity.supplierId = :supplierId)" +
            "AND (" +
            "    LOWER(p.shortName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%'))" +
            ") " +
            "ORDER BY p.productId DESC")
    Page<ProductEntity> searchProducts(
            @Param("clientId") Long clientId,
            @Param("search") String search,
            Pageable pageable,
            @Param("supplierId") Long supplierId
    );

    @Query("""
            SELECT DISTINCT p FROM ProductEntity p
            JOIN p.productPricesEntity prices
            WHERE p.clientEntity.clientId = :clientId
              AND (:supplierId IS NULL OR p.supplierEntity.supplierId = :supplierId)
              AND p.deletedAt IS NULL
            ORDER BY p.fullName
        """)
    Page<ProductEntity> findAllWithPricesByClientId(@Param("clientId") Long clientId, Pageable pageable, @Param("supplierId") Long supplierId);

    @Query("""
    SELECT p FROM ProductEntity p
    WHERE (p.fullName = :fullName OR p.shortName = :shortName OR p.productId = :productId)
      AND p.clientEntity.clientId = :clientId
      AND p.deletedAt IS NULL
""")
    ProductEntity findFirstActiveProduct(String fullName, String shortName, Long productId, Long clientId);

    @Transactional(propagation = Propagation.MANDATORY)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    ProductEntity findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(String fullname, String shortName, Long clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "javax.persistence.lock.timeout", value = "5000")
    })
    @Query("""
    SELECT p
    FROM ProductEntity p
    WHERE p.fullName = :fullname
      AND p.shortName = :shortName
      AND p.deletedAt IS NULL
      AND p.clientEntity.clientId = :clientId
""")
    ProductEntity findAndLockProduct(
            @Param("fullname") String fullname,
            @Param("shortName") String shortName,
            @Param("clientId") Long clientId
    );

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

    @Query("SELECT SUM(p.stock * p.supplierPrice) FROM ProductEntity p WHERE p.clientEntity.clientId = :clientId")
    BigDecimal sumInventoryValue(@Param("clientId") Long clientId);

    List<ProductEntity> findAllByClientEntity_ClientIdAndShortNameInAndProductPricesEntityIsNotNullAndDeletedAtIsNull(Long clientId, Set<String> shortNames);


    @Query("""
    SELECT p FROM ProductEntity p
    LEFT JOIN FETCH p.productPricesEntity
    WHERE p.clientEntity.clientId = :clientId
    AND p.deletedAt IS NULL
    AND (
        LOWER(p.shortName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    ORDER BY p.fullName
""")
    List<ProductEntity> findAllWithPricesByClientId(
            @Param("clientId") Long clientId,
            @Param("keyword") String keyword
    );


    // Search by shortName only
    @Query("""
    SELECT p FROM ProductEntity p
    LEFT JOIN FETCH p.productPricesEntity
    WHERE p.clientEntity.clientId = :clientId
    AND LOWER(p.shortName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    AND p.deletedAt IS NULL
""")
    List<ProductEntity> findByShortNameContaining(
            @Param("clientId") Long clientId,
            @Param("keyword") String keyword
    );

    // Search by fullName only
    @Query("""
    SELECT p FROM ProductEntity p
    LEFT JOIN FETCH p.productPricesEntity
    WHERE p.clientEntity.clientId = :clientId
    AND LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    AND p.deletedAt IS NULL
    ORDER BY p.fullName
""")
    List<ProductEntity> findByFullNameContaining(
            @Param("clientId") Long clientId,
            @Param("keyword") String keyword
    );

    ProductEntity findByShortNameAndClientEntity_ClientIdAndDeletedAtIsNull(String shortName, Long clientId);

    @Query("""
    SELECT p FROM ProductEntity p
    WHERE p.clientEntity.clientId = :clientId
    AND (:supplierId IS NULL OR p.supplierEntity.supplierId = :supplierId)
    AND p.minimumStock > 0
    AND p.stock <= p.minimumStock
    AND p.deletedAt IS NULL
    ORDER BY p.fullName
""")
    List<ProductEntity> getUnderstockProductData(Long clientId, Long supplierId);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE product
        SET stock = stock - :qty
        WHERE product_id = :productId
        RETURNING stock
        """, nativeQuery = true)
    List<Long> reduceStockReturning(@Param("productId") Long productId,
                                    @Param("qty") Long qty);
}
