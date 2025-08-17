package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreorderRepository extends JpaRepository<PreorderEntity, String> {
    List<PreorderEntity> findAllByClientEntity_ClientIdOrderByPreorderIdAsc(String clientId);
    List<PreorderEntity> findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdOrderByPreorderIdAsc(String clientId, String supplierId);

    Optional<PreorderEntity> findFirstByOrderByPreorderIdDesc();

    PreorderEntity findFirstByPreorderIdAndDeletedAtIsNull(String preorderId);
}
