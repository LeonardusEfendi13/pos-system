package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByTransactionIdDesc(Long clientId);

    List<TransactionEntity> findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(Long clientId, Long customerId, LocalDateTime StartDate, LocalDateTime endDate);

    List<TransactionEntity> findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(Long clientId, LocalDateTime StartDate, LocalDateTime endDate);

    Optional<TransactionEntity> findFirstByClientEntity_ClientIdAndTransactionIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNull(Long clientId, Long transactionId);
}
