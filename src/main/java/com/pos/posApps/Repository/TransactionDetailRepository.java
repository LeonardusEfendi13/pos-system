package com.pos.posApps.Repository;

import com.pos.posApps.DTO.Dtos.Home.HomeProductDTO;
import com.pos.posApps.Entity.TransactionDetailEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetailEntity, Long> {
    List<TransactionDetailEntity> findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(Long transactionId);
    void deleteAllByTransactionEntity_TransactionId(Long transactionId);

    @Query("""
    SELECT new com.pos.posApps.DTO.Dtos.Home.HomeProductDTO(
        p.fullName,
        SUM(td.qty)
    )
    FROM TransactionDetailEntity td
    JOIN ProductEntity p ON p.shortName = td.shortName
    WHERE td.deletedAt IS NULL
      AND p.deletedAt IS NULL
      AND td.createdAt BETWEEN :startDate AND :endDate
    GROUP BY p.fullName
    ORDER BY SUM(td.qty) DESC
""")
    List<HomeProductDTO> findTopProducts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query(value = """
            SELECT
                d.product_id AS product_id,
                COALESCE(
                    NULLIF(p.full_name, ''),
                    NULLIF(MAX(d.full_name), ''),
                    NULLIF(p.short_name, ''),
                    NULLIF(MAX(d.short_name), ''),
                    'Tanpa nama'
                ) AS product_name,
                COALESCE(NULLIF(p.short_name, ''), NULLIF(MAX(d.short_name), ''), '') AS short_name,
                COALESCE(MAX(s.supplier_name), '') AS supplier_name,
                SUM(d.qty) AS qty,
                SUM(d.total_price) AS total_harga,
                SUM(COALESCE(d.total_profit, 0)) AS laba
            FROM transaction t
            JOIN transaction_detail d ON d.transaction_id = t.transaction_id
            LEFT JOIN product p
                ON p.product_id = d.product_id
               AND p.client_id = t.client_id
            LEFT JOIN supplier s ON s.supplier_id = p.supplier_id
            WHERE t.client_id = :clientId
              AND t.deleted_at IS NULL
              AND d.deleted_at IS NULL
              AND d.product_id IS NOT NULL
              AND t.created_at BETWEEN :startDate AND :endDate
              AND (:supplierId IS NULL OR p.supplier_id = :supplierId)
            GROUP BY d.product_id, p.full_name, p.short_name
            HAVING SUM(d.qty) > 0
            """, nativeQuery = true)
    List<Object[]> findPenjualanPerBarang(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("supplierId") Long supplierId
    );

    @Query(value = """
            SELECT
                CASE
                    WHEN :filter = 'year' THEN TO_CHAR(t.created_at, 'YYYY')
                    WHEN :filter = 'month' THEN TO_CHAR(t.created_at, 'YYYY-MM')
                    ELSE TO_CHAR(t.created_at, 'YYYY-MM-DD')
                END AS period,
                SUM(d.qty) AS qty,
                SUM(d.total_price) AS total_harga,
                SUM(COALESCE(d.total_profit, 0)) AS laba
            FROM transaction t
            JOIN transaction_detail d ON d.transaction_id = t.transaction_id
            WHERE t.client_id = :clientId
              AND d.product_id = :productId
              AND t.deleted_at IS NULL
              AND d.deleted_at IS NULL
              AND t.created_at BETWEEN :startDate AND :endDate
            GROUP BY period
            ORDER BY period ASC
            """, nativeQuery = true)
    List<Object[]> findPenjualanBarangPerWaktu(
            @Param("clientId") Long clientId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("filter") String filter
    );

    @Query(value = """
            SELECT
                COALESCE(
                    NULLIF(MAX(d.full_name), ''),
                    NULLIF(MAX(d.short_name), ''),
                    'Tanpa nama'
                ) AS product_name
            FROM transaction_detail d
            JOIN transaction t ON t.transaction_id = d.transaction_id
            WHERE t.client_id = :clientId
              AND d.product_id = :productId
              AND t.deleted_at IS NULL
              AND d.deleted_at IS NULL
            HAVING COUNT(d.transaction_detail_id) > 0
            """, nativeQuery = true)
    List<String> findSoldProductLabel(
            @Param("clientId") Long clientId,
            @Param("productId") Long productId
    );
}
