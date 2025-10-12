package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PurchasingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchasingRepository extends JpaRepository<PurchasingEntity, Long> {
    List<PurchasingEntity> findAllByClientEntity_ClientIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(Long clientId, LocalDateTime startDate, LocalDateTime endDate);

    List<PurchasingEntity> findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(Long clientId, Long supplierId, LocalDateTime startDate, LocalDateTime endDate);

    Optional<PurchasingEntity> findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(Long clientId, Long purchasingId);

    Optional<PurchasingEntity> findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByPurchasingIdDesc(Long clientId);

    Optional<PurchasingEntity> findFirstByClientEntity_ClientIdAndPurchasingNumberAndSupplierEntity_SupplierId(Long clientId, String purchasingNumber, Long supplierId);

    Optional<PurchasingEntity> findFirstByPurchasingNumberAndClientEntity_ClientIdAndDeletedAtIsNull(String purchasingNumber, Long clientId);

    boolean existsByPurchasingNumberAndClientEntity_ClientIdAndDeletedAtIsNullAndPurchasingIdNot(String purchasingNumber, Long clientId, Long purchasingId);
}