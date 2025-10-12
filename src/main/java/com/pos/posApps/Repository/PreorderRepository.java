package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PreorderRepository extends JpaRepository<PreorderEntity, Long> {
    Page<PreorderEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByPreorderIdDesc(Long clientId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<PreorderEntity> findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByPreorderIdDesc(Long clientId, Long supplierId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Optional<PreorderEntity> findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByPreorderIdDesc(Long clientId);
    Optional<PreorderEntity> findFirstByClientEntity_ClientIdAndPreorderIdAndPreorderDetailEntitiesIsNotNullAndDeletedAtIsNull(Long clientId, Long preorderId);
}
