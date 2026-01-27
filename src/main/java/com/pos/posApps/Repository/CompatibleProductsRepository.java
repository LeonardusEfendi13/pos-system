package com.pos.posApps.Repository;

import com.pos.posApps.Entity.CompatibleProductsEntity;
import com.pos.posApps.Entity.ProductEntity;
import com.pos.posApps.Entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompatibleProductsRepository extends JpaRepository<CompatibleProductsEntity, Long> {

    void deleteAllByProductEntity_ProductId(Long productId);

    // =====================================================
    // VEHICLE + PRODUCT KEYWORD
    // =====================================================
    @Query("""
        SELECT DISTINCT cp.productEntity
        FROM CompatibleProductsEntity cp
        JOIN cp.productEntity p
        WHERE cp.isValid = true
        AND cp.vehicleEntity.id IN :vehicleIds
        AND (
            LOWER(p.shortName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    """)
    List<ProductEntity> searchByVehiclesAndProductKeyword(
            @Param("vehicleIds") List<Long> vehicleIds,
            @Param("keyword") String keyword
    );

    // =====================================================
    // VEHICLE ONLY (tanpa keyword produk)
    // =====================================================
    @Query("""
        SELECT DISTINCT cp.productEntity
        FROM CompatibleProductsEntity cp
        WHERE cp.isValid = true
        AND cp.vehicleEntity.id IN :vehicleIds
    """)
    List<ProductEntity> findProductsByVehicles(
            @Param("vehicleIds") List<Long> vehicleIds
    );
}
