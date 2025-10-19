package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.SuggestedPricesDTO;
import com.pos.posApps.Entity.PriceListEntity;
import com.pos.posApps.Repository.PriceListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static com.pos.posApps.Constants.Constant.*;

@Service
public class PriceListService {
    @Autowired
    PriceListRepository priceListRepository;

    public SuggestedPricesDTO getSuggestedPriceByPartNumber(String partNumber){
        //Clean the part number
        partNumber = partNumber.trim().toUpperCase();
        Optional<PriceListEntity> priceListEntityOpt = priceListRepository.findAllByPartNumber(partNumber);
        if(priceListEntityOpt.isEmpty()){
            return new SuggestedPricesDTO();
        }

        PriceListEntity priceListEntity = priceListEntityOpt.get();
        //Initiate Suggested price
        BigDecimal suggestedPrice = null;
        BigDecimal basicPrice = null;


        //Get Merk
        String merk = priceListEntity.getMerk();
        BigDecimal hargaJual = priceListEntity.getHargaJual();
        BigDecimal yamahaPercentageDisc = yamahaDisc.divide(BigDecimal.valueOf(100),3, RoundingMode.HALF_UP);
        BigDecimal hondaPercentageDisc = hondaDisc.divide(BigDecimal.valueOf(100),3, RoundingMode.HALF_UP);
        BigDecimal profitPercentage = profitPct.divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);

        if(merk.equalsIgnoreCase("YAMAHA")){
            basicPrice = (hargaJual.subtract(hargaJual.multiply(yamahaPercentageDisc)));
            suggestedPrice = basicPrice.add(basicPrice.multiply(profitPercentage));
        }else if (merk.equalsIgnoreCase("HONDA")){
            basicPrice = (hargaJual.subtract(hargaJual.multiply(hondaPercentageDisc)));
            suggestedPrice = basicPrice.add(basicPrice.multiply(profitPercentage));
        }
        if(suggestedPrice != null){
            return new SuggestedPricesDTO(suggestedPrice.stripTrailingZeros().toPlainString(), basicPrice.stripTrailingZeros().toPlainString());
        }
        return new SuggestedPricesDTO();
    }
}
