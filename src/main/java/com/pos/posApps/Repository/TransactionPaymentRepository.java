package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TransactionPaymentRepository extends JpaRepository<TransactionPaymentEntity, Long> {
    void deleteAllByTransactionEntity_TransactionId(Long transactionId);

    List<TransactionPaymentEntity> findByTransactionEntity_TransactionIdOrderBySortOrderAsc(
            Long transactionId
    );

    List<TransactionPaymentEntity> findByTransactionEntity_TransactionIdIn(
            Collection<Long> transactionIds
    );
}
