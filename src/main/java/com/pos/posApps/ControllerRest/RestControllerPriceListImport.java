package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.PriceListImportJobDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Service.PriceListImportService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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

    /** Starts the replace as a background job; poll /confirm/{jobId} for progress. */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestParam("merk") String merk) {
        PriceListImportService.StartResult result = priceListImportService.startConfirm(merk);
        return switch (result.status()) {
            case ACCEPTED -> ResponseEntity.status(HttpStatus.ACCEPTED).body(result.job());
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(result.job() == null
                    ? new ResponseInBoolean(false, result.message())
                    : Map.of("status", false, "message", result.message(), "job", result.job()));
            case REJECTED -> ResponseEntity.badRequest().body(new ResponseInBoolean(false, result.message()));
        };
    }

    @GetMapping("/confirm/active")
    public ResponseEntity<PriceListImportJobDTO> activeJob() {
        return priceListImportService.activeJob()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/confirm/{jobId}")
    public ResponseEntity<?> job(@PathVariable("jobId") String jobId) {
        return priceListImportService.job(jobId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseInBoolean(false, "Status impor tidak ditemukan.")));
    }
}
