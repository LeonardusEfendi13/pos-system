package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetailEntity, Long> {
    Optional<TransactionDetailEntity> findFirstByDeletedAtIsNullOrderByTransactionDetailIdDesc();

    List<TransactionDetailEntity> findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(Long transactionId);
    List<TransactionDetailEntity> findAllByDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionDetailIdDesc(LocalDateTime startDate, LocalDateTime endDate);

    void deleteAllByTransactionEntity_TransactionId(Long transactionId);
}
