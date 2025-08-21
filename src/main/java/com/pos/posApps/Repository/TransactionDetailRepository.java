package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetailEntity, String> {
    Optional<TransactionDetailEntity> findFirstByOrderByCreatedAtDesc();

    List<TransactionDetailEntity> findAllByTransactionEntity_TransactionIdOrderByCreatedAtDesc(String transactionId);

    void deleteAllByTransactionEntity_TransactionId(String transactionId);
}
