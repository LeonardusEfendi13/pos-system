package com.pos.posApps.Repository;

import com.pos.posApps.Entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, String> {
    SupplierEntity findFirstBySupplierId(String supplierId);

    List<SupplierEntity> findAllByClientEntity_ClientId(String clientId);

    SupplierEntity findFirstBySupplierNameAndClientEntity_ClientId(String supplierName, String clientId);

    SupplierEntity findFirstByOrderBySupplierNameDesc();

    SupplierEntity findFirstBySupplierIdAndClientEntity_ClientId(String supplierId, String clientId);
}
