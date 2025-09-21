package com.pos.posApps.Repository;

import com.pos.posApps.Entity.StockMovementsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockMovementsRepository extends JpaRepository<StockMovementsEntity, Long> {
    List<StockMovementsEntity> findAllByClientEntity_ClientIdAndProductEntity_ProductIdAndCreatedAtBetweenAndDeletedAtIsNullOrderByStockMovementsIdAsc(Long clientId, Long productId, LocalDateTime startDate, LocalDateTime endDate);
    Optional<StockMovementsEntity> findFirstByDeletedAtIsNullOrderByStockMovementsIdDesc();

    // Ambil semua transaksi dalam periode
    List<StockMovementsEntity> findByProductEntity_ProductIdAndDeletedAtIsNullAndCreatedAtBefore(Long productId, LocalDateTime startDate);
}
