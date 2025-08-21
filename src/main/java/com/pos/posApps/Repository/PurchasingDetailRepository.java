package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PurchasingDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchasingDetailRepository extends JpaRepository<PurchasingDetailEntity, Long> {
}
