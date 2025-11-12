package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PurchasingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchasingRepository extends JpaRepository<PurchasingEntity, Long> {
    @Query("SELECT p FROM PurchasingEntity p " +
            "WHERE p.clientEntity.clientId = :clientId " +
            "AND p.purchasingDetailEntities IS NOT EMPTY " +
            "AND p.deletedAt IS NULL " +
            "AND p.poDate BETWEEN :startDate AND :endDate " +
            "AND (:supplierId IS NULL OR p.supplierEntity.supplierId = :supplierId) " +
            "AND (:lunas IS NULL OR p.isPaid = :lunas) " +
            "AND (:tunai IS NULL OR p.isCash = :tunai) " +
            "AND (" +
            "    LOWER(p.purchasingNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(p.supplierEntity.supplierName) LIKE LOWER(CONCAT('%', :search, '%'))" +
            ") " +
            "ORDER BY p.purchasingId DESC")
    Page<PurchasingEntity> searchPurchasings(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("supplierId") Long supplierId,
            @Param("lunas") Boolean lunas,
            @Param("tunai") Boolean tunai,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
    SELECT p FROM PurchasingEntity p
    WHERE p.clientEntity.clientId = :clientId
      AND (:supplierId IS NULL OR p.supplierEntity.supplierId = :supplierId)
      AND (:lunas IS NULL OR p.isPaid = :lunas)
      AND (:tunai IS NULL OR p.isCash = :tunai)
      AND p.purchasingDetailEntities IS NOT EMPTY
      AND p.deletedAt IS NULL
      AND p.poDate BETWEEN :startDate AND :endDate
    ORDER BY p.purchasingId DESC
""")
    Page<PurchasingEntity> findPurchasingData(
            @Param("clientId") Long clientId,
            @Param("supplierId") Long supplierId,
            @Param("lunas") Boolean lunas,
            @Param("tunai") Boolean tunai,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    List<PurchasingEntity> findAllByClientEntity_ClientIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(Long clientId, LocalDateTime startDate, LocalDateTime endDate);

    List<PurchasingEntity> findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(Long clientId, Long supplierId, LocalDateTime startDate, LocalDateTime endDate);

    Optional<PurchasingEntity> findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(Long clientId, Long purchasingId);

    Optional<PurchasingEntity> findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByPurchasingIdDesc(Long clientId);

    Optional<PurchasingEntity> findFirstByClientEntity_ClientIdAndPurchasingNumberAndSupplierEntity_SupplierId(Long clientId, String purchasingNumber, Long supplierId);

    Optional<PurchasingEntity> findFirstByPurchasingNumberAndClientEntity_ClientIdAndDeletedAtIsNull(String purchasingNumber, Long clientId);

    boolean existsByPurchasingNumberAndClientEntity_ClientIdAndDeletedAtIsNullAndPurchasingIdNot(String purchasingNumber, Long clientId, Long purchasingId);
}