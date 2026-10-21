package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DashboardCustomerRepository extends JpaRepository<TransactionEntity, Long> {
    @Query(value = """
            SELECT t.customer_id,
                   COALESCE(SUM(COALESCE(t.total_price, 0) - d.hpp), 0)
            FROM transaction t
            JOIN (
                SELECT transaction_id,
                       SUM(total_price - COALESCE(total_profit, 0)) AS hpp
                FROM transaction_detail
                WHERE deleted_at IS NULL
                GROUP BY transaction_id
            ) d ON d.transaction_id = t.transaction_id
            WHERE t.client_id = :clientId
              AND t.deleted_at IS NULL
              AND t.created_at BETWEEN :startDate AND :endDate
              AND NOT EXISTS (
                  SELECT 1
                  FROM branch b
                  WHERE b.customer_id = t.customer_id
              )
            GROUP BY t.customer_id
            """, nativeQuery = true)
    List<Object[]> sumProfitByCustomer(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}
