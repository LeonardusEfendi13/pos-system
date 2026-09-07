package com.pos.posApps.Repository;

import com.pos.posApps.DTO.Dtos.Home.SupplierRankingView;
import com.pos.posApps.Entity.PurchasingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DashboardSupplierRepository extends JpaRepository<PurchasingEntity, Long> {
    @Query(value = """
            SELECT s.supplier_name AS name,
                   COUNT(p.purchasing_id) AS invoice_count,
                   COALESCE(SUM(p.total_price), 0) AS total_spending,
                   COALESCE(SUM(CASE WHEN p.is_paid = FALSE THEN p.total_price ELSE 0 END), 0) AS unpaid_total
            FROM purchasing p
            INNER JOIN supplier s ON s.supplier_id = p.supplier_id
            WHERE p.client_id = :clientId
              AND p.deleted_at IS NULL
              AND p.po_date BETWEEN :startDate AND :endDate
              AND p.supplier_id IS NOT NULL
            GROUP BY s.supplier_id, s.supplier_name
            ORDER BY total_spending DESC, s.supplier_name ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<SupplierRankingView> findTopSuppliers(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("limit") int limit
    );
}
