package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ProductSetComponentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSetComponentRepository extends JpaRepository<ProductSetComponentEntity, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM ProductSetComponentEntity c
            WHERE c.deletedAt IS NULL
              AND c.productSet.deletedAt IS NULL
              AND c.productSet.clientEntity.clientId = :clientId
              AND c.childProduct.productId = :productId
              AND c.productSet.productSetId <> :excludeSetId
            """)
    boolean isActiveComponent(
            @Param("clientId") Long clientId,
            @Param("productId") Long productId,
            @Param("excludeSetId") Long excludeSetId
    );
}
