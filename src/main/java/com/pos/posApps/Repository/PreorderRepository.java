package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreorderRepository extends JpaRepository<PreorderEntity, Long> {
    List<PreorderEntity> findAllByClientEntity_ClientIdOrderByPreorderIdDesc(Long clientId);
    List<PreorderEntity> findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdOrderByPreorderIdDesc(Long clientId, Long supplierId);

    Optional<PreorderEntity> findFirstByOrderByPreorderIdDesc();

    PreorderEntity findFirstByPreorderIdAndDeletedAtIsNull(Long preorderId);
}
