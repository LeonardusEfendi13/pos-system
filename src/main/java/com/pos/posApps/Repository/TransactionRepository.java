package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Integer> {
    Optional<TransactionEntity> findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByCreatedAtDesc(String clientId);

    List<TransactionEntity> findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(String clientId, LocalDateTime StartDate, LocalDateTime endDate);

    TransactionEntity findFirstByClientEntity_ClientIdAndTransactionIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNull(String clientId, String transactionId);
}
