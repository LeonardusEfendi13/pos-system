package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.AdjustStockDTO;
import com.pos.posApps.Entity.StockMovementsEntity;
import com.pos.posApps.Repository.StockMovementsRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMovementService {
    @Autowired
    private StockMovementsRepository stockMovementsRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void insertKartuStok(AdjustStockDTO adjustStockDTO) {
        Long lastStockMovementId = stockMovementsRepository.findFirstByDeletedAtIsNullOrderByStockMovementsIdDesc().map(StockMovementsEntity::getStockMovementsId).orElse(0L);
        Long newStockMovementId = Generator.generateId(lastStockMovementId);

        StockMovementsEntity stockMovementsEntity = new StockMovementsEntity();

        stockMovementsEntity.setStockMovementsId(newStockMovementId);
        stockMovementsEntity.setReferenceNo(adjustStockDTO.getReferenceNo());
        stockMovementsEntity.setTipeKartuStok(adjustStockDTO.getTipeKartuStok());
        stockMovementsEntity.setQtyIn(adjustStockDTO.getQtyIn());
        stockMovementsEntity.setQtyOut(adjustStockDTO.getQtyOut());
        stockMovementsEntity.setSaldo(adjustStockDTO.getSaldo());
        stockMovementsEntity.setProductEntity(adjustStockDTO.getProductEntity());
        stockMovementsEntity.setClientEntity(adjustStockDTO.getClientData());

        stockMovementsRepository.save(stockMovementsEntity);
    }
}
