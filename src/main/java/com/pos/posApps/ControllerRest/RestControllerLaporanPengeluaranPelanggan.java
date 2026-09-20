package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.LaporanPembelianPerPelangganDTO;
import com.pos.posApps.DTO.Dtos.LaporanPengeluaranPelangganPageDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.LaporanService;
import com.pos.posApps.Util.LaporanPengeluaranPelangganPdfExporter;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/laporan")
@AllArgsConstructor
public class RestControllerLaporanPengeluaranPelanggan {
    private AuthService authService;
    private LaporanService laporanService;
    private LaporanPengeluaranPelangganPdfExporter pdfExporter;

    @GetMapping("/pengeluaran/pelanggan")
    public ResponseEntity<LaporanPengeluaranPelangganPageDTO> pengeluaranPelanggan(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();

            ResolvedFilter resolved = resolveFilter(startDate, endDate);
            List<LaporanPembelianPerPelangganDTO> rows = loadRows(clientId, resolved);

            return ResponseEntity.ok(new LaporanPengeluaranPelangganPageDTO(
                    rows,
                    sumTotal(rows),
                    resolved.startDate.toString(),
                    resolved.endDate.toString(),
                    account.getRole().name(),
                    account.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @GetMapping("/pengeluaran/pelanggan/pdf")
    public ResponseEntity<StreamingResponseBody> pengeluaranPelangganPdf(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();
            ResolvedFilter resolved = resolveFilter(startDate, endDate);
            List<LaporanPembelianPerPelangganDTO> rows = loadRows(clientId, resolved);

            StreamingResponseBody stream = outputStream -> pdfExporter.export(
                    rows,
                    resolved.startDate,
                    resolved.endDate,
                    outputStream
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laporan_pembelian_per_supplier.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(stream);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    private List<LaporanPembelianPerPelangganDTO> loadRows(Long clientId, ResolvedFilter resolved) {
        LocalDateTime inputStartDate = resolved.startDate.atStartOfDay();
        LocalDateTime inputEndDate = resolved.endDate.atTime(23, 59, 59);

        return laporanService.getLaporanPembelianDataByCustomer(
                clientId,
                inputStartDate,
                inputEndDate
        );
    }

    private ResolvedFilter resolveFilter(String startDate, String endDate) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedStart = parseDate(startDate, today.withDayOfMonth(1));
        LocalDate resolvedEnd = parseDate(endDate, today);

        if (resolvedEnd.isBefore(resolvedStart)) {
            resolvedEnd = resolvedStart;
        }

        return new ResolvedFilter(resolvedStart, resolvedEnd);
    }

    private LocalDate parseDate(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private BigDecimal sumTotal(List<LaporanPembelianPerPelangganDTO> rows) {
        return rows.stream()
                .map(row -> row.getTotalHargaPembelian() == null
                        ? BigDecimal.ZERO
                        : row.getTotalHargaPembelian())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record ResolvedFilter(LocalDate startDate, LocalDate endDate) {
    }
}
