package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CustomerLookupDTO;
import com.pos.posApps.DTO.Dtos.LaporanPendapatanPeriodePageDTO;
import com.pos.posApps.DTO.Dtos.LaporanPenjualanPerWaktuDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.CustomerService;
import com.pos.posApps.Service.LaporanService;
import com.pos.posApps.Util.LaporanPendapatanPeriodePdfExporter;
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
public class RestControllerLaporanPendapatanPeriode {
    private static final Set<String> FILTER_OPTIONS = Set.of("day", "month", "year");

    private AuthService authService;
    private CustomerService customerService;
    private LaporanService laporanService;
    private LaporanPendapatanPeriodePdfExporter pdfExporter;

    @GetMapping("/pendapatan/periode")
    public ResponseEntity<LaporanPendapatanPeriodePageDTO> pendapatanPeriode(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String filterOptions) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();

            ResolvedFilter resolved = resolveFilter(startDate, endDate, customerId, filterOptions);
            List<LaporanPenjualanPerWaktuDTO> rows = loadRows(clientId, resolved);
            List<CustomerLookupDTO> customers = customerService.getCustomerList(clientId).stream()
                    .map(this::toCustomerLookup)
                    .toList();

            return ResponseEntity.ok(new LaporanPendapatanPeriodePageDTO(
                    rows,
                    sumKotor(rows),
                    sumBersih(rows),
                    customers,
                    resolved.startDate.toString(),
                    resolved.endDate.toString(),
                    resolved.customerId,
                    resolved.filterOptions,
                    account.getRole().name(),
                    account.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @GetMapping("/pendapatan/periode/pdf")
    public ResponseEntity<StreamingResponseBody> pendapatanPeriodePdf(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String filterOptions) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();
            ResolvedFilter resolved = resolveFilter(startDate, endDate, customerId, filterOptions);
            List<LaporanPenjualanPerWaktuDTO> rows = loadRows(clientId, resolved);

            StreamingResponseBody stream = outputStream -> pdfExporter.export(
                    rows,
                    resolved.startDate,
                    resolved.endDate,
                    resolved.filterOptions,
                    outputStream
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laporan_pendapatan_per_periode.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(stream);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    private List<LaporanPenjualanPerWaktuDTO> loadRows(Long clientId, ResolvedFilter resolved) {
        LocalDateTime inputStartDate = resolved.startDate.atStartOfDay();
        LocalDateTime inputEndDate = resolved.endDate.atTime(23, 59, 59);

        return laporanService.getLaporanPenjualanDataByPeriode(
                clientId,
                inputStartDate,
                inputEndDate,
                resolved.customerId,
                resolved.filterOptions
        );
    }

    private ResolvedFilter resolveFilter(
            String startDate,
            String endDate,
            Long customerId,
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
                customerId,
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

    private CustomerLookupDTO toCustomerLookup(CustomerEntity customer) {
        return new CustomerLookupDTO(
                customer.getCustomerId(),
                customer.getName() == null ? "" : customer.getName()
        );
    }

    private BigDecimal sumKotor(List<LaporanPenjualanPerWaktuDTO> rows) {
        return rows.stream()
                .map(row -> row.getTotalHargaPenjualan() == null
                        ? BigDecimal.ZERO
                        : row.getTotalHargaPenjualan())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumBersih(List<LaporanPenjualanPerWaktuDTO> rows) {
        return rows.stream()
                .map(row -> row.getLabaPenjualan() == null
                        ? BigDecimal.ZERO
                        : row.getLabaPenjualan())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record ResolvedFilter(
            LocalDate startDate,
            LocalDate endDate,
            Long customerId,
            String filterOptions
    ) {
    }
}
