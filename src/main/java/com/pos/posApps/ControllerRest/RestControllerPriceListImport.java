package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Service.PriceListImportService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/data-center/price-list")
@AllArgsConstructor
public class RestControllerPriceListImport {
    private final PriceListImportService priceListImportService;

    @PostMapping("/preview")
    public ResponseEntity<?> preview(
            @RequestParam("merk") String merk,
            @RequestParam("file") MultipartFile file
    ) {
        PriceListImportService.PreviewResult result = priceListImportService.preview(merk, file);
        if (result.error() != null) {
            return ResponseEntity.badRequest().body(new ResponseInBoolean(false, result.error()));
        }
        return ResponseEntity.ok(result.preview());
    }

    @PostMapping("/confirm")
    public ResponseEntity<ResponseInBoolean> confirm(@RequestParam("merk") String merk) {
        ResponseInBoolean result = priceListImportService.confirm(merk);
        if (!result.isStatus()) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
