package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PreorderRepository extends JpaRepository<PreorderEntity, Long> {
    List<PreorderEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByPreorderIdDesc(Long clientId, LocalDateTime startDate, LocalDateTime endDate);
    List<PreorderEntity> findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByPreorderIdDesc(Long clientId, Long supplierId, LocalDateTime startDate, LocalDateTime endDate);
    Optional<PreorderEntity> findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByPreorderIdDesc(Long clientId);

    PreorderEntity findFirstByPreorderIdAndDeletedAtIsNull(Long preorderId);
}
