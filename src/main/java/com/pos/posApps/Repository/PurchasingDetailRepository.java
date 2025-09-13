package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PurchasingDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchasingDetailRepository extends JpaRepository<PurchasingDetailEntity, Long> {
    Optional<PurchasingDetailEntity> findFirstByDeletedAtIsNullOrderByPurchasingDetailIdDesc();
    List<PurchasingDetailEntity> findAllByPurchasingEntity_PurchasingIdOrderByPurchasingDetailIdDesc(Long purchasingId);
    void deleteAllByPurchasingEntity_PurchasingId(Long purchasingId);
}
