package com.pos.posApps.Repository;

import com.pos.posApps.Entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, String> {
    SupplierEntity findFirstBySupplierIdAndDeletedAtIsNull(String supplierId);

    List<SupplierEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByCreatedAtDesc(String clientId);

    SupplierEntity findFirstBySupplierNameIgnoreCaseAndClientEntity_ClientIdAndDeletedAtIsNull(String supplierName, String clientId);

    SupplierEntity findFirstByOrderByCreatedAtDesc();

    SupplierEntity findFirstBySupplierIdAndClientEntity_ClientIdAndDeletedAtIsNull(String supplierId, String clientId);

    boolean existsBySupplierNameIgnoreCaseAndClientEntity_ClientIdAndDeletedAtIsNullAndSupplierIdNot(String supplierName, String clientId, String supplierId);
}
