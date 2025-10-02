package com.pos.posApps.ControllerRest;

import com.pos.posApps.Service.PriceListService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/price-list")
@AllArgsConstructor
public class RestControllerPriceList {

    private PriceListService priceListService;

    @GetMapping("/cek")
    public ResponseEntity<String> cekNewPrice(@RequestParam("kode") String partNumber){
        String newPrice = priceListService.getSuggestedPriceByPartNumber(partNumber);
        return ResponseEntity.ok(newPrice);
    }
}
