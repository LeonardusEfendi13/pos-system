package com.pos.posApps.Repository;

import com.pos.posApps.Entity.BuktiBayarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BuktiBayarRepository extends JpaRepository<BuktiBayarEntity, Long> {
    Optional<BuktiBayarEntity> findByPurchasingEntity_PurchasingId(Long pembelianId);

    long countByFilePathAndBuktiBayarIdNot(String filePath, Long buktiBayarId);

    Optional<BuktiBayarEntity> findByTransactionEntity_TransactionId(Long transactionId);
}
