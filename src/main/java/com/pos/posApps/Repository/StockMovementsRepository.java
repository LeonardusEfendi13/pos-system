package com.pos.posApps.Repository;

import com.pos.posApps.DTO.Dtos.StockMovementsDTO;
import com.pos.posApps.Entity.StockMovementsEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementsRepository extends JpaRepository<StockMovementsEntity, Long> {
    String KARTU_STOK_RANGE = """
            WHERE s.clientEntity.clientId = :clientId
              AND s.productEntity.productId = :productId
              AND s.deletedAt IS NULL
              AND s.createdAt BETWEEN :startDate AND :endDate
            """;

    List<StockMovementsEntity> findAllByClientEntity_ClientIdAndProductEntity_ProductIdAndCreatedAtBetweenAndDeletedAtIsNullOrderByStockMovementsIdAsc(Long clientId, Long productId, LocalDateTime startDate, LocalDateTime endDate);

    List<StockMovementsEntity> findByProductEntity_ProductIdAndDeletedAtIsNullAndCreatedAtBefore(Long productId, LocalDateTime startDate);

    @Query("""
            SELECT new com.pos.posApps.DTO.Dtos.StockMovementsDTO(
                s.stockMovementsId,
                s.referenceNo,
                s.tipeKartuStok,
                s.qtyIn,
                s.qtyOut,
                s.saldo,
                s.createdAt,
                s.invDate
            )
            FROM StockMovementsEntity s
            """ + KARTU_STOK_RANGE + """
            ORDER BY s.stockMovementsId ASC
            """)
    List<StockMovementsDTO> findKartuStok(
            @Param("clientId") Long clientId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
            SELECT
                COALESCE(SUM(qty_in), 0),
                COALESCE(SUM(qty_out), 0),
                COALESCE(SUM(CASE
                    WHEN tipe = :pretel
                    THEN COALESCE(qty_in, 0) + COALESCE(qty_out, 0)
                    WHEN tipe = :koreksiPretel
                    THEN -(COALESCE(qty_in, 0) + COALESCE(qty_out, 0))
                    ELSE 0 END), 0)
            FROM stock_movements
            WHERE client_id = :clientId
              AND product_id = :productId
              AND deleted_at IS NULL
              AND created_at BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    List<Object[]> summarizeKartuStok(
            @Param("clientId") Long clientId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("pretel") String pretel,
            @Param("koreksiPretel") String koreksiPretel
    );

    @Query("""
            SELECT s.saldo FROM StockMovementsEntity s
            """ + KARTU_STOK_RANGE + """
            ORDER BY s.stockMovementsId DESC
            """)
    List<Long> findLastSaldoInRange(
            @Param("clientId") Long clientId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
            SELECT s.saldo FROM StockMovementsEntity s
            WHERE s.productEntity.productId = :productId
              AND s.deletedAt IS NULL
              AND s.createdAt < :startDate
            ORDER BY s.createdAt DESC, s.stockMovementsId DESC
            """)
    List<Long> findOpeningSaldo(
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            Pageable pageable
    );
}
