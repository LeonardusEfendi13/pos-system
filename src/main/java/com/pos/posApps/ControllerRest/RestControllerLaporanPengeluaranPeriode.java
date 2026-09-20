package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.LaporanPembelianPerWaktuDTO;
import com.pos.posApps.DTO.Dtos.LaporanPengeluaranPeriodePageDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.LaporanService;
import com.pos.posApps.Util.LaporanPengeluaranPeriodePdfExporter;
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
import java.util.Locale;
import java.util.Set;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/laporan")
@AllArgsConstructor
public class RestControllerLaporanPengeluaranPeriode {
    private static final Set<String> FILTER_OPTIONS = Set.of("day", "month", "year");

    private AuthService authService;
    private LaporanService laporanService;
    private LaporanPengeluaranPeriodePdfExporter pdfExporter;

    @GetMapping("/pengeluaran/periode")
    public ResponseEntity<LaporanPengeluaranPeriodePageDTO> pengeluaranPeriode(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String filterOptions) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();

            ResolvedFilter resolved = resolveFilter(startDate, endDate, filterOptions);
            List<LaporanPembelianPerWaktuDTO> rows = loadRows(clientId, resolved);

            return ResponseEntity.ok(new LaporanPengeluaranPeriodePageDTO(
                    rows,
                    sumPembelian(rows),
                    resolved.startDate.toString(),
                    resolved.endDate.toString(),
                    resolved.filterOptions,
                    account.getRole().name(),
                    account.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @GetMapping("/pengeluaran/periode/pdf")
    public ResponseEntity<StreamingResponseBody> pengeluaranPeriodePdf(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String filterOptions) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();
            ResolvedFilter resolved = resolveFilter(startDate, endDate, filterOptions);
            List<LaporanPembelianPerWaktuDTO> rows = loadRows(clientId, resolved);

            StreamingResponseBody stream = outputStream -> pdfExporter.export(
                    rows,
                    resolved.startDate,
                    resolved.endDate,
                    resolved.filterOptions,
                    outputStream
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laporan_pembelian_per_periode.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(stream);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    private List<LaporanPembelianPerWaktuDTO> loadRows(Long clientId, ResolvedFilter resolved) {
        LocalDateTime inputStartDate = resolved.startDate.atStartOfDay();
        LocalDateTime inputEndDate = resolved.endDate.atTime(23, 59, 59);

        return laporanService.getLaporanPengeluaranDataByPeriode(
                clientId,
                inputStartDate,
                inputEndDate,
                null,
                resolved.filterOptions
        );
    }

    private ResolvedFilter resolveFilter(
            String startDate,
            String endDate,
            String filterOptions) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedStart = parseDate(startDate, today.withDayOfMonth(1));
        LocalDate resolvedEnd = parseDate(endDate, today);

        if (resolvedEnd.isBefore(resolvedStart)) {
            resolvedEnd = resolvedStart;
        }

        return new ResolvedFilter(
                resolvedStart,
                resolvedEnd,
                resolveFilterOptions(filterOptions)
        );
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

    private String resolveFilterOptions(String raw) {
        if (raw == null || raw.isBlank()) {
            return "day";
        }

        String normalized = raw.toLowerCase(Locale.ROOT);
        return FILTER_OPTIONS.contains(normalized) ? normalized : "day";
    }

    private BigDecimal sumPembelian(List<LaporanPembelianPerWaktuDTO> rows) {
        return rows.stream()
                .map(row -> row.getTotalHargaPembelian() == null
                        ? BigDecimal.ZERO
                        : row.getTotalHargaPembelian())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record ResolvedFilter(
            LocalDate startDate,
            LocalDate endDate,
            String filterOptions
    ) {
    }
}
