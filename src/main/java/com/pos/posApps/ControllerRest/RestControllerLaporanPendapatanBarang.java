package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.LaporanPendapatanBarangDetailDTO;
import com.pos.posApps.DTO.Dtos.LaporanPendapatanBarangPageDTO;
import com.pos.posApps.DTO.Dtos.LaporanPenjualanPerBarangDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.LaporanService;
import com.pos.posApps.Util.LaporanPendapatanBarangDetailPdfExporter;
import com.pos.posApps.Util.LaporanPendapatanBarangPdfExporter;
import com.pos.posApps.Util.LaporanPenjualanBarangView;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/laporan")
@AllArgsConstructor
public class RestControllerLaporanPendapatanBarang {
    private AuthService authService;
    private LaporanService laporanService;
    private LaporanPendapatanBarangPdfExporter pdfExporter;
    private LaporanPendapatanBarangDetailPdfExporter detailPdfExporter;

    @GetMapping("/pendapatan/barang")
    public ResponseEntity<LaporanPendapatanBarangPageDTO> pendapatanBarang(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir
    ) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        Long clientId = account.getClientEntity().getClientId();
        ResolvedFilter resolved = resolveFilter(startDate, endDate);
        List<LaporanPenjualanPerBarangDTO> rows = loadRows(clientId, resolved, supplierId, q, sort, dir);

        return ResponseEntity.ok(new LaporanPendapatanBarangPageDTO(
                rows,
                sumQty(rows),
                sumKotor(rows),
                sumBersih(rows),
                resolved.startDate.toString(),
                resolved.endDate.toString(),
                account.getRole().name(),
                account.getName()
        ));
    }

    @GetMapping("/pendapatan/barang/pdf")
    public ResponseEntity<StreamingResponseBody> pendapatanBarangPdf(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir
    ) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        Long clientId = account.getClientEntity().getClientId();
        ResolvedFilter resolved = resolveFilter(startDate, endDate);
        List<LaporanPenjualanPerBarangDTO> rows = loadRows(clientId, resolved, supplierId, q, sort, dir);
        String supplierName = laporanService.resolveBarangSupplierName(clientId, supplierId);

        StreamingResponseBody stream = outputStream -> pdfExporter.export(
                rows,
                resolved.startDate,
                resolved.endDate,
                supplierName,
                q,
                outputStream
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=laporan_penjualan_per_barang.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(stream);
    }

    @GetMapping("/pendapatan/barang/{productId}")
    public ResponseEntity<LaporanPendapatanBarangDetailDTO> pendapatanBarangDetail(
            HttpSession session,
            @PathVariable Long productId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String filterOptions
    ) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        LaporanPendapatanBarangDetailDTO detail = loadDetail(
                account,
                productId,
                startDate,
                endDate,
                filterOptions
        );
        if (detail == null) {
            return ResponseEntity.status(NOT_FOUND).build();
        }

        return ResponseEntity.ok(detail);
    }

    @GetMapping("/pendapatan/barang/{productId}/pdf")
    public ResponseEntity<StreamingResponseBody> pendapatanBarangDetailPdf(
            HttpSession session,
            @PathVariable Long productId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String filterOptions
    ) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        LaporanPendapatanBarangDetailDTO detail = loadDetail(
                account,
                productId,
                startDate,
                endDate,
                filterOptions
        );
        if (detail == null) {
            return ResponseEntity.status(NOT_FOUND).build();
        }

        StreamingResponseBody stream = outputStream -> detailPdfExporter.export(
                detail.getProductName(),
                detail.getSupplierName(),
                detail.getContent(),
                LocalDate.parse(detail.getStartDate()),
                LocalDate.parse(detail.getEndDate()),
                detail.getFilterOptions(),
                outputStream
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=laporan_penjualan_barang_" + productId + ".pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(stream);
    }

    private LaporanPendapatanBarangDetailDTO loadDetail(
            AccountEntity account,
            Long productId,
            String startDate,
            String endDate,
            String filterOptions
    ) {
        ResolvedFilter resolved = resolveFilter(startDate, endDate);
        return laporanService.getLaporanPenjualanBarangDetail(
                account.getClientEntity().getClientId(),
                productId,
                resolved.startDate.atStartOfDay(),
                resolved.endDate.atTime(23, 59, 59),
                LaporanPenjualanBarangView.normalizeFilter(filterOptions),
                account.getRole().name(),
                account.getName()
        );
    }

    private List<LaporanPenjualanPerBarangDTO> loadRows(
            Long clientId,
            ResolvedFilter resolved,
            Long supplierId,
            String q,
            String sort,
            String dir
    ) {
        return laporanService.getLaporanPenjualanPerBarang(
                clientId,
                resolved.startDate.atStartOfDay(),
                resolved.endDate.atTime(23, 59, 59),
                supplierId,
                q,
                sort,
                dir
        );
    }

    private AccountEntity requireAccount(HttpSession session) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            return authService.validateToken(token);
        } catch (Exception e) {
            return null;
        }
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

    private Long sumQty(List<LaporanPenjualanPerBarangDTO> rows) {
        long total = 0L;
        for (LaporanPenjualanPerBarangDTO row : rows) {
            total += row.getQty() == null ? 0L : row.getQty();
        }
        return total;
    }

    private BigDecimal sumKotor(List<LaporanPenjualanPerBarangDTO> rows) {
        return rows.stream()
                .map(row -> row.getTotalHargaPenjualan() == null
                        ? BigDecimal.ZERO
                        : row.getTotalHargaPenjualan())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumBersih(List<LaporanPenjualanPerBarangDTO> rows) {
        return rows.stream()
                .map(row -> row.getLabaPenjualan() == null
                        ? BigDecimal.ZERO
                        : row.getLabaPenjualan())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record ResolvedFilter(LocalDate startDate, LocalDate endDate) {
    }
}
