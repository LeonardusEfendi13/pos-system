package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.AdjustStockDTO;
import com.pos.posApps.Entity.StockMovementsEntity;
import com.pos.posApps.Repository.StockMovementsRepository;
import com.pos.posApps.Util.Generator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockMovementService {
    @Autowired
    private StockMovementsRepository stockMovementsRepository;

    @Transactional
    public boolean insertKartuStok(AdjustStockDTO adjustStockDTO) {
        try{
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
            return true;
        }catch (Exception e){
            System.out.println("Error when adjusting stock : " + e.getMessage());
            return false;
        }
    }
}
