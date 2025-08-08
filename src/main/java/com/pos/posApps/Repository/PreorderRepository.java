package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreorderRepository extends JpaRepository<PreorderEntity, String> {
    List<PreorderEntity> findAllByClientEntity_ClientId(String clientId);
    List<PreorderEntity> findAllByClientEntity_ClientIdAndSupplierEntity_SupplierId(String clientId, String supplierId);

    PreorderEntity findFirstByOrderByPreorderIdDesc();

}
