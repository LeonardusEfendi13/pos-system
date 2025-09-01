package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PurchasingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchasingRepository extends JpaRepository<PurchasingEntity, Long> {
    List<PurchasingEntity> findAllByClientEntity_ClientIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(Long clientId, LocalDateTime startDate, LocalDateTime endDate);
    List<PurchasingEntity> findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(Long clientId, Long supplierId, LocalDateTime startDate, LocalDateTime endDate);
    PurchasingEntity findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(Long clientId, Long purchasingId);
}
