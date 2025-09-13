package com.pos.posApps.Repository;

import com.pos.posApps.Entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {
    Optional<SupplierEntity> findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(Long supplierId, Long clientId);

    List<SupplierEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderBySupplierIdDesc(Long clientId);

    SupplierEntity findFirstBySupplierNameIgnoreCaseAndClientEntity_ClientIdAndDeletedAtIsNull(String supplierName, Long clientId);

    SupplierEntity findFirstByOrderBySupplierIdDesc();

    SupplierEntity findFirstBySupplierIdAndClientEntity_ClientIdAndDeletedAtIsNull(Long supplierId, Long clientId);

    boolean existsBySupplierNameIgnoreCaseAndClientEntity_ClientIdAndDeletedAtIsNullAndSupplierIdNot(String supplierName, Long clientId, Long supplierId);
}
