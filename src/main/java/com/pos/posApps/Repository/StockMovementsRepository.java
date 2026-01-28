package com.pos.posApps.Repository;

import com.pos.posApps.Entity.StockMovementsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementsRepository extends JpaRepository<StockMovementsEntity, Long> {
    List<StockMovementsEntity> findAllByClientEntity_ClientIdAndProductEntity_ProductIdAndCreatedAtBetweenAndDeletedAtIsNullOrderByStockMovementsIdAsc(Long clientId, Long productId, LocalDateTime startDate, LocalDateTime endDate);
    List<StockMovementsEntity> findByProductEntity_ProductIdAndDeletedAtIsNullAndCreatedAtBefore(Long productId, LocalDateTime startDate);
}
