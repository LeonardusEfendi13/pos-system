package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PreorderRepository extends JpaRepository<PreorderEntity, Long> {
    @Query("SELECT p FROM PreorderEntity p " +
            "WHERE p.clientEntity.clientId = :clientId " +
            "AND p.deletedAt IS NULL " +
            "AND p.createdAt BETWEEN :startDate AND :endDate " +
            "AND (:supplierId IS NULL OR p.supplierEntity.supplierId = :supplierId) " +
            "AND (" +
            "    LOWER(p.supplierEntity.supplierName) LIKE LOWER(CONCAT('%', :search, '%'))" +
            "    OR CAST(p.preorderId AS string) LIKE CONCAT('%', :search, '%')" +
            ") " +
            "ORDER BY p.preorderId DESC")
    Page<PreorderEntity> searchPreorders(
            @Param("clientId") Long clientId,
            @Param("supplierId") Long supplierId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            Pageable pageable
    );

    Optional<PreorderEntity> findFirstByClientEntity_ClientIdAndPreorderIdAndDeletedAtIsNull(Long clientId, Long preorderId);

    Page<PreorderEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByPreorderIdDesc(Long clientId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<PreorderEntity> findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByPreorderIdDesc(Long clientId, Long supplierId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Optional<PreorderEntity> findFirstByClientEntity_ClientIdAndPreorderIdAndPreorderDetailEntitiesIsNotNullAndDeletedAtIsNull(Long clientId, Long preorderId);
}
