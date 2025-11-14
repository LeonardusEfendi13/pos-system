package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.clientEntity.clientId = :clientId " +
            "AND t.transactionDetailEntities IS NOT EMPTY " +
            "AND t.deletedAt IS NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "AND (:customerId IS NULL OR t.customerEntity.customerId = :customerId) " +
            "AND (" +
            "    LOWER(t.transactionNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.customerEntity.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
            ") " +
            "ORDER BY t.transactionId DESC")
    Page<TransactionEntity> searchTransactions(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("customerId") Long customerId,
            @Param("search") String search,
            Pageable pageable
    );

    Optional<TransactionEntity> findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByTransactionIdDesc(Long clientId);

    List<TransactionEntity> findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(Long clientId, Long customerId, LocalDateTime StartDate, LocalDateTime endDate);

    List<TransactionEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(Long clientId, LocalDateTime StartDate, LocalDateTime endDate);

    Page<TransactionEntity> findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(Long clientId, Long customerId, LocalDateTime StartDate, LocalDateTime endDate, Pageable pageable);

    Page<TransactionEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(Long clientId, LocalDateTime StartDate, LocalDateTime endDate, Pageable pageable);

    Optional<TransactionEntity> findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(Long clientId, Long transactionId);

    Optional<TransactionEntity> findFirstByClientEntity_ClientIdAndDeletedAtIsNullAndTransactionNumberStartingWithOrderByTransactionNumberDesc(Long clientId, String today);
}
