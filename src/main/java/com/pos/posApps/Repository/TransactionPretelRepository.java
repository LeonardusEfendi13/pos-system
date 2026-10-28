package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionPretelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionPretelRepository extends JpaRepository<TransactionPretelEntity, Long> {
    List<TransactionPretelEntity> findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionPretelIdAsc(
            Long transactionId
    );
}
