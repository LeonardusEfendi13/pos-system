package com.pos.posApps.Repository;

import com.pos.posApps.DTO.Dtos.ChartPointView;
import com.pos.posApps.DTO.Dtos.HutangPenjualanTotalsDTO;
import com.pos.posApps.Entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
            "AND (:customerId IS NULL OR t.customerEntity.customerId IN :customerId) " +
            "AND (" +
            "    LOWER(t.transactionNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.customerEntity.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
            ") " +
            "ORDER BY t.transactionId DESC")
    Page<TransactionEntity> searchTransactions(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("customerId") List<Long> customerId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.clientEntity.clientId = :clientId " +
            "AND t.transactionDetailEntities IS NOT EMPTY " +
            "AND t.deletedAt IS NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "AND t.customerEntity.customerId NOT IN :excludeCustomerId " +
            "AND (" +
            "    LOWER(t.transactionNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.customerEntity.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
            ") " +
            "ORDER BY t.transactionId DESC")
    Page<TransactionEntity> searchTransactionsExcludingCustomers(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("excludeCustomerId") List<Long> excludeCustomerId,
            @Param("search") String search,
            Pageable pageable
    );


    @Query(value = """
    SELECT t.*
    FROM transaction t
    WHERE t.client_id = :clientId
      AND NOT EXISTS (
          SELECT 1
          FROM branch b
          WHERE b.customer_id = t.customer_id
      )
      AND t.deleted_at IS NULL
      AND t.created_at BETWEEN :startDate AND :endDate
    ORDER BY t.transaction_id DESC
""", nativeQuery = true)
    List<TransactionEntity> findFilteredTransactions(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query(value = """
    SELECT t.*
    FROM transaction t
    WHERE t.client_id = :clientId
      AND NOT EXISTS (
          SELECT 1
          FROM branch b
          WHERE b.customer_id = t.customer_id
      )
      AND t.deleted_at IS NULL
      AND t.is_paid = FALSE
    ORDER BY t.transaction_id DESC
""", nativeQuery = true)
    List<TransactionEntity> findUnpaidTransactions(Long clientId);

    Page<TransactionEntity> findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdInAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(Long clientId, List<Long> customerId, LocalDateTime StartDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.clientEntity.clientId = :clientId " +
            "AND t.deletedAt IS NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "AND t.customerEntity.customerId NOT IN :excludeCustomerId " +
            "ORDER BY t.transactionId DESC")
    Page<TransactionEntity> findExcludingCustomers(
            @Param("clientId") Long clientId,
            @Param("excludeCustomerId") List<Long> excludeCustomerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

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
              AND NOT EXISTS (
                          SELECT 1
                          FROM branch b
                          WHERE b.customer_id = t.customer_id
                      )
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
                    c.customer_id AS customerId,
                    COALESCE(c.name, 'Unknown Customer') AS customerName,
                    SUM(d.total_price) AS totalHarga,
                    SUM(COALESCE(d.total_profit, 0)) AS laba,
                    COUNT(DISTINCT t.transaction_id) AS transactionCount,
                    COUNT(DISTINCT CASE WHEN t.is_paid = FALSE THEN t.transaction_id END) AS unpaidInvoiceCount
                FROM transaction t
                JOIN transaction_detail d ON d.transaction_id = t.transaction_id
                LEFT JOIN customer c ON c.customer_id = t.customer_id
                WHERE t.client_id = :clientId
                  AND t.deleted_at IS NULL
                  AND d.deleted_at IS NULL
                  AND t.created_at BETWEEN :startDate AND :endDate
                GROUP BY c.customer_id, c.name
                ORDER BY totalHarga DESC
            """, nativeQuery = true)
    List<Object[]> getLaporanPenjualanRaw(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query(value = """
                SELECT
                    t.customer_id AS customerId,
                    CASE
                        WHEN t.is_cash = FALSE THEN 'credit'
                        WHEN tp.transaction_payment_id IS NOT NULL
                            THEN COALESCE(NULLIF(LOWER(tp.payment_method_type), ''), 'cash')
                        ELSE COALESCE(NULLIF(LOWER(t.payment_method_type), ''), 'cash')
                    END AS method_type,
                    CASE
                        WHEN t.is_cash = FALSE THEN COALESCE(NULLIF(t.payment_type_name, ''), 'Kredit')
                        WHEN tp.transaction_payment_id IS NOT NULL
                            THEN COALESCE(NULLIF(tp.payment_type_name, ''), 'Cash')
                        ELSE COALESCE(NULLIF(t.payment_type_name, ''), 'Cash')
                    END AS type_name,
                    CASE
                        WHEN t.is_cash = FALSE THEN COALESCE(NULLIF(t.payment_type_name, ''), 'Kredit')
                        WHEN tp.transaction_payment_id IS NOT NULL
                            THEN COALESCE(NULLIF(tp.payment_method_name, ''), NULLIF(tp.payment_type_name, ''), 'Cash')
                        ELSE COALESCE(NULLIF(t.payment_method_name, ''), NULLIF(t.payment_type_name, ''), 'Cash')
                    END AS method_name,
                    SUM(
                        CASE
                            WHEN t.is_cash = FALSE THEN COALESCE(t.total_price, 0)
                            WHEN tp.transaction_payment_id IS NOT NULL THEN COALESCE(tp.amount, 0)
                            ELSE COALESCE(t.total_price, 0)
                        END
                    ) AS amount
                FROM transaction t
                LEFT JOIN transaction_payment tp
                    ON tp.transaction_id = t.transaction_id
                   AND t.is_cash = TRUE
                WHERE t.client_id = :clientId
                  AND t.deleted_at IS NULL
                  AND t.created_at BETWEEN :startDate AND :endDate
                GROUP BY 1, 2, 3, 4
            """, nativeQuery = true)
    List<Object[]> getLaporanPenjualanMetodePerPelangganRaw(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query(value = """
                SELECT
                    t.customer_id AS customerId,
                    SUM(COALESCE(t.total_price, 0)) AS unpaidTotal
                FROM transaction t
                WHERE t.client_id = :clientId
                  AND t.deleted_at IS NULL
                  AND t.is_paid = FALSE
                  AND t.created_at BETWEEN :startDate AND :endDate
                GROUP BY t.customer_id
            """, nativeQuery = true)
    List<Object[]> sumUnpaidTotalByCustomer(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("""
            SELECT t FROM TransactionEntity t
            WHERE t.clientEntity.clientId = :clientId
              AND t.deletedAt IS NULL
              AND t.customerEntity.customerId = :customerId
              AND t.createdAt BETWEEN :startDate AND :endDate
            ORDER BY t.createdAt DESC, t.transactionId DESC
            """)
    List<TransactionEntity> findCustomerTransactions(
            @Param("clientId") Long clientId,
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
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
             AND NOT EXISTS (
                      SELECT 1
                      FROM branch b
                      WHERE b.customer_id = t.customer_id
                  )
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

    @Query(value = """
            SELECT
                CASE
                    WHEN :filter = 'year'  THEN TO_CHAR(t.created_at, 'YYYY')
                    WHEN :filter = 'month' THEN TO_CHAR(t.created_at, 'YYYY-MM')
                    ELSE TO_CHAR(t.created_at, 'YYYY-MM-DD')
                END AS period,
                CASE
                    WHEN t.is_cash = FALSE THEN 'credit'
                    WHEN tp.transaction_payment_id IS NOT NULL
                        THEN COALESCE(NULLIF(LOWER(tp.payment_method_type), ''), 'cash')
                    ELSE COALESCE(NULLIF(LOWER(t.payment_method_type), ''), 'cash')
                END AS method_type,
                CASE
                    WHEN t.is_cash = FALSE THEN COALESCE(NULLIF(t.payment_type_name, ''), 'Kredit')
                    WHEN tp.transaction_payment_id IS NOT NULL
                        THEN COALESCE(NULLIF(tp.payment_type_name, ''), 'Cash')
                    ELSE COALESCE(NULLIF(t.payment_type_name, ''), 'Cash')
                END AS type_name,
                CASE
                    WHEN t.is_cash = FALSE THEN COALESCE(NULLIF(t.payment_type_name, ''), 'Kredit')
                    WHEN tp.transaction_payment_id IS NOT NULL
                        THEN COALESCE(NULLIF(tp.payment_method_name, ''), NULLIF(tp.payment_type_name, ''), 'Cash')
                    ELSE COALESCE(NULLIF(t.payment_method_name, ''), NULLIF(t.payment_type_name, ''), 'Cash')
                END AS method_name,
                SUM(
                    CASE
                        WHEN t.is_cash = FALSE THEN COALESCE(t.total_price, 0)
                        WHEN tp.transaction_payment_id IS NOT NULL THEN COALESCE(tp.amount, 0)
                        ELSE COALESCE(t.total_price, 0)
                    END
                ) AS amount
            FROM transaction t
            LEFT JOIN transaction_payment tp
                ON tp.transaction_id = t.transaction_id
               AND t.is_cash = TRUE
            WHERE t.client_id = :clientId
              AND NOT EXISTS (
                    SELECT 1
                    FROM branch b
                    WHERE b.customer_id = t.customer_id
              )
              AND t.deleted_at IS NULL
              AND t.created_at BETWEEN :startDate AND :endDate
              AND (:customerId IS NULL OR t.customer_id = :customerId)
            GROUP BY 1, 2, 3, 4
            ORDER BY 1 DESC
            """, nativeQuery = true)
    List<Object[]> getLaporanPenjualanMetodePerWaktuRaw(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long customerId,
            String filter
    );

    String PENJUALAN_HISTORY_WHERE = """
            WHERE t.clientEntity.clientId = :clientId
              AND t.deletedAt IS NULL
              AND t.createdAt BETWEEN :startDate AND :endDate
              AND (:cash IS NULL OR t.isCash = :cash)
              AND (:paid IS NULL OR t.isPaid = :paid)
              AND (
                :search = ''
                OR LOWER(t.transactionNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(t.customerEntity.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR EXISTS (
                  SELECT 1 FROM TransactionDetailEntity d
                  WHERE d.transactionEntity = t
                    AND d.deletedAt IS NULL
                    AND (
                      LOWER(d.shortName) LIKE LOWER(CONCAT('%', :search, '%'))
                      OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                )
              )
            """;

    @EntityGraph(attributePaths = {"customerEntity", "accountEntity"})
    @Query(
            value = "SELECT t FROM TransactionEntity t " + PENJUALAN_HISTORY_WHERE +
                    "AND (:customerId IS NULL OR t.customerEntity.customerId = :customerId)",
            countQuery = "SELECT COUNT(t) FROM TransactionEntity t " + PENJUALAN_HISTORY_WHERE +
                    "AND (:customerId IS NULL OR t.customerEntity.customerId = :customerId)"
    )
    Page<TransactionEntity> findPenjualanHistoryPage(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("customerId") Long customerId,
            @Param("cash") Boolean cash,
            @Param("paid") Boolean paid,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customerEntity", "accountEntity"})
    @Query(
            value = "SELECT t FROM TransactionEntity t " + PENJUALAN_HISTORY_WHERE +
                    "AND t.customerEntity.customerId NOT IN :excludeCustomerIds",
            countQuery = "SELECT COUNT(t) FROM TransactionEntity t " + PENJUALAN_HISTORY_WHERE +
                    "AND t.customerEntity.customerId NOT IN :excludeCustomerIds"
    )
    Page<TransactionEntity> findPenjualanHistoryPageExcludingCustomers(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("excludeCustomerIds") List<Long> excludeCustomerIds,
            @Param("cash") Boolean cash,
            @Param("paid") Boolean paid,
            @Param("search") String search,
            Pageable pageable
    );

    // Deleted sales: their detail lines are soft-deleted with them, so the
    // product search does not filter detail deletedAt
    String PENJUALAN_DELETED_WHERE = """
            WHERE t.clientEntity.clientId = :clientId
              AND t.deletedAt IS NOT NULL
              AND t.createdAt BETWEEN :startDate AND :endDate
              AND (:cash IS NULL OR t.isCash = :cash)
              AND (:paid IS NULL OR t.isPaid = :paid)
              AND (
                :search = ''
                OR LOWER(t.transactionNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(t.customerEntity.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR EXISTS (
                  SELECT 1 FROM TransactionDetailEntity d
                  WHERE d.transactionEntity = t
                    AND (
                      LOWER(d.shortName) LIKE LOWER(CONCAT('%', :search, '%'))
                      OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                )
              )
            """;

    @EntityGraph(attributePaths = {"customerEntity", "accountEntity"})
    @Query(
            value = "SELECT t FROM TransactionEntity t " + PENJUALAN_DELETED_WHERE +
                    "AND (:customerId IS NULL OR t.customerEntity.customerId = :customerId)",
            countQuery = "SELECT COUNT(t) FROM TransactionEntity t " + PENJUALAN_DELETED_WHERE +
                    "AND (:customerId IS NULL OR t.customerEntity.customerId = :customerId)"
    )
    Page<TransactionEntity> findPenjualanDeletedPage(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("customerId") Long customerId,
            @Param("cash") Boolean cash,
            @Param("paid") Boolean paid,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customerEntity", "accountEntity"})
    @Query(
            value = "SELECT t FROM TransactionEntity t " + PENJUALAN_DELETED_WHERE +
                    "AND t.customerEntity.customerId NOT IN :excludeCustomerIds",
            countQuery = "SELECT COUNT(t) FROM TransactionEntity t " + PENJUALAN_DELETED_WHERE +
                    "AND t.customerEntity.customerId NOT IN :excludeCustomerIds"
    )
    Page<TransactionEntity> findPenjualanDeletedPageExcludingCustomers(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("excludeCustomerIds") List<Long> excludeCustomerIds,
            @Param("cash") Boolean cash,
            @Param("paid") Boolean paid,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(t.totalPrice), 0) FROM TransactionEntity t " + PENJUALAN_DELETED_WHERE +
            "AND (:customerId IS NULL OR t.customerEntity.customerId = :customerId)")
    BigDecimal sumPenjualanDeleted(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("customerId") Long customerId,
            @Param("cash") Boolean cash,
            @Param("paid") Boolean paid,
            @Param("search") String search
    );

    @Query("SELECT COALESCE(SUM(t.totalPrice), 0) FROM TransactionEntity t " + PENJUALAN_DELETED_WHERE +
            "AND t.customerEntity.customerId NOT IN :excludeCustomerIds")
    BigDecimal sumPenjualanDeletedExcludingCustomers(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("excludeCustomerIds") List<Long> excludeCustomerIds,
            @Param("cash") Boolean cash,
            @Param("paid") Boolean paid,
            @Param("search") String search
    );

    Optional<TransactionEntity> findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNotNull(
            Long clientId,
            Long transactionId
    );

    Optional<TransactionEntity> findFirstByClientEntity_ClientIdAndTransactionNumberAndDeletedAtIsNull(
            Long clientId,
            String transactionNumber
    );

    String HUTANG_PENJUALAN_WHERE = """
            WHERE t.clientEntity.clientId = :clientId
              AND t.deletedAt IS NULL
              AND t.isCash = false
              AND (:applyStart = 0 OR t.createdAt >= :startDate)
              AND (:applyEnd = 0 OR t.createdAt <= :endDate)
              AND (:paid IS NULL OR t.isPaid = :paid)
              AND (
                :search = ''
                OR LOWER(t.transactionNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(t.customerEntity.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR EXISTS (
                  SELECT 1 FROM TransactionDetailEntity d
                  WHERE d.transactionEntity = t
                    AND d.deletedAt IS NULL
                    AND (
                      LOWER(d.shortName) LIKE LOWER(CONCAT('%', :search, '%'))
                      OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                )
              )
            """;

    String HUTANG_PENJUALAN_TOTALS = """
            SELECT new com.pos.posApps.DTO.Dtos.HutangPenjualanTotalsDTO(
                COALESCE(SUM(t.totalPrice), 0),
                COALESCE(SUM(COALESCE(t.paidAmount, 0)), 0),
                COALESCE(SUM(
                    CASE
                        WHEN COALESCE(t.totalPrice, 0) > COALESCE(t.paidAmount, 0)
                        THEN COALESCE(t.totalPrice, 0) - COALESCE(t.paidAmount, 0)
                        ELSE 0
                    END
                ), 0)
            )
            FROM TransactionEntity t
            """;

    @EntityGraph(attributePaths = {"customerEntity", "accountEntity"})
    @Query(
            value = "SELECT t FROM TransactionEntity t " + HUTANG_PENJUALAN_WHERE +
                    "AND (:customerId IS NULL OR t.customerEntity.customerId = :customerId)",
            countQuery = "SELECT COUNT(t) FROM TransactionEntity t " + HUTANG_PENJUALAN_WHERE +
                    "AND (:customerId IS NULL OR t.customerEntity.customerId = :customerId)"
    )
    Page<TransactionEntity> findHutangPenjualanPage(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("applyStart") int applyStart,
            @Param("applyEnd") int applyEnd,
            @Param("customerId") Long customerId,
            @Param("paid") Boolean paid,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customerEntity", "accountEntity"})
    @Query(
            value = "SELECT t FROM TransactionEntity t " + HUTANG_PENJUALAN_WHERE +
                    "AND t.customerEntity.customerId NOT IN :excludeCustomerIds",
            countQuery = "SELECT COUNT(t) FROM TransactionEntity t " + HUTANG_PENJUALAN_WHERE +
                    "AND t.customerEntity.customerId NOT IN :excludeCustomerIds"
    )
    Page<TransactionEntity> findHutangPenjualanPageExcludingCustomers(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("applyStart") int applyStart,
            @Param("applyEnd") int applyEnd,
            @Param("excludeCustomerIds") List<Long> excludeCustomerIds,
            @Param("paid") Boolean paid,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            HUTANG_PENJUALAN_TOTALS + HUTANG_PENJUALAN_WHERE +
                    "AND (:customerId IS NULL OR t.customerEntity.customerId = :customerId)"
    )
    HutangPenjualanTotalsDTO summarizeHutangPenjualan(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("applyStart") int applyStart,
            @Param("applyEnd") int applyEnd,
            @Param("customerId") Long customerId,
            @Param("paid") Boolean paid,
            @Param("search") String search
    );

    @Query(
            HUTANG_PENJUALAN_TOTALS + HUTANG_PENJUALAN_WHERE +
                    "AND t.customerEntity.customerId NOT IN :excludeCustomerIds"
    )
    HutangPenjualanTotalsDTO summarizeHutangPenjualanExcludingCustomers(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("applyStart") int applyStart,
            @Param("applyEnd") int applyEnd,
            @Param("excludeCustomerIds") List<Long> excludeCustomerIds,
            @Param("paid") Boolean paid,
            @Param("search") String search
    );
}
