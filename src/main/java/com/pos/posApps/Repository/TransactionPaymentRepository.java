package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionPaymentRepository extends JpaRepository<TransactionPaymentEntity, Long> {
    void deleteAllByTransactionEntity_TransactionId(Long transactionId);
}
