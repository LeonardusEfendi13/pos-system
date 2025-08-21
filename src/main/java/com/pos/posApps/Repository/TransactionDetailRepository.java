package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetailEntity, Long> {
    Optional<TransactionDetailEntity> findFirstByOrderByTransactionDetailIdDesc();

    List<TransactionDetailEntity> findAllByTransactionEntity_TransactionIdOrderByTransactionDetailIdDesc(Long transactionId);

    void deleteAllByTransactionEntity_TransactionId(Long transactionId);
}
