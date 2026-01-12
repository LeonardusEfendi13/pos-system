package com.pos.posApps.Repository;

import com.pos.posApps.DTO.Dtos.ChartPointView;
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

//    Optional<TransactionEntity> findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByTransactionIdDesc(Long clientId);

    List<TransactionEntity> findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(Long clientId, Long customerId, LocalDateTime StartDate, LocalDateTime endDate);

    List<TransactionEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(Long clientId, LocalDateTime StartDate, LocalDateTime endDate);

    Page<TransactionEntity> findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(Long clientId, Long customerId, LocalDateTime StartDate, LocalDateTime endDate, Pageable pageable);

    Page<TransactionEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(Long clientId, LocalDateTime StartDate, LocalDateTime endDate, Pageable pageable);

    Optional<TransactionEntity> findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(Long clientId, Long transactionId);

    @Query(value = """
                SELECT TO_CHAR(t.created_at, :dateFormat) AS label,
                       SUM(td.total_profit) AS value
                FROM transaction t
                JOIN transaction_detail td
                  ON t.transaction_id = td.transaction_id
                WHERE t.client_id = :clientId
                  AND t.deleted_at IS NULL
                  AND t.created_at BETWEEN :start AND :end
                GROUP BY label
                ORDER BY label
            """, nativeQuery = true)
    List<ChartPointView> getLabaChart(
            Long clientId,
            LocalDateTime start,
            LocalDateTime end,
            String dateFormat
    );

    @Query(value = """
                SELECT
                    TO_CHAR(t.created_at, :dateFormat) AS label,
                    SUM(t.total_price) AS value
                FROM transaction t
                WHERE t.client_id = :clientId
                  AND t.deleted_at IS NULL
                  AND t.created_at BETWEEN :start AND :end
                GROUP BY label
                ORDER BY label
            """, nativeQuery = true)
    List<ChartPointView> getPendapatanChart(
            Long clientId,
            LocalDateTime start,
            LocalDateTime end,
            String dateFormat
    );

    @Query(value = """
                SELECT 
                    COALESCE(c.name, 'Unknown Customer') AS customerName,
                    SUM(d.total_price)                  AS totalHarga,
                    SUM(COALESCE(d.total_profit, 0))    AS laba
                FROM transaction t
                JOIN transaction_detail d ON d.transaction_id = t.transaction_id
                LEFT JOIN customer c ON c.customer_id = t.customer_id
                WHERE t.client_id = :clientId
                  AND t.deleted_at IS NULL
                  AND d.deleted_at IS NULL
                  AND t.created_at BETWEEN :startDate AND :endDate
                GROUP BY c.name
                ORDER BY totalHarga DESC
            """, nativeQuery = true)
    List<Object[]> getLaporanPenjualanRaw(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query(value = """
            SELECT
                CASE
                    WHEN :filter = 'year'  THEN TO_CHAR(t.created_at, 'YYYY')
                    WHEN :filter = 'month' THEN TO_CHAR(t.created_at, 'YYYY-MM')
                    ELSE TO_CHAR(t.created_at, 'YYYY-MM-DD')
                END AS period,
                SUM(d.total_price)               AS totalHarga,
                SUM(COALESCE(d.total_profit, 0)) AS laba
            FROM transaction t
            JOIN transaction_detail d
                 ON d.transaction_id = t.transaction_id
            WHERE t.client_id = :clientId
              AND t.deleted_at IS NULL
              AND d.deleted_at IS NULL
              AND t.created_at BETWEEN :startDate AND :endDate
              AND (:customerId IS NULL OR t.customer_id = :customerId)
            GROUP BY period
            ORDER BY period DESC
            """, nativeQuery = true)
    List<Object[]> getLaporanPenjualanPerWaktuRaw(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long customerId,
            String filter
    );
}
