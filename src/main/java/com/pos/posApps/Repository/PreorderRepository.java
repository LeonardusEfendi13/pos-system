package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreorderRepository extends JpaRepository<PreorderEntity, String> {
    List<PreorderEntity> findAllByClientEntity_ClientIdOrderByCreatedAtDesc(String clientId);
    List<PreorderEntity> findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdOrderByCreatedAtDesc(String clientId, String supplierId);

    Optional<PreorderEntity> findFirstByOrderByCreatedAtDesc();

    PreorderEntity findFirstByPreorderIdAndDeletedAtIsNull(String preorderId);
}
