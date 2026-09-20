package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.LaporanNilaiPersediaanDTO;
import com.pos.posApps.DTO.Dtos.LaporanNilaiPersediaanPageDTO;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.LaporanService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/laporan")
@AllArgsConstructor
public class RestControllerLaporanNilaiPersediaan {
    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> SORTABLE = Set.of(
            "shortName",
            "fullName",
            "qty",
            "supplierPrice",
            "hargaJual",
            "totalPrice"
    );

    private AuthService authService;
    private LaporanService laporanService;

    @GetMapping("/nilai-persediaan")
    public ResponseEntity<LaporanNilaiPersediaanPageDTO> nilaiPersediaan(
            HttpSession session,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            var account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();

            int safePage = page == null || page < 0 ? 0 : page;
            int safeSize = size == null || size < 1 ? 10 : Math.min(size, MAX_PAGE_SIZE);

            List<LaporanNilaiPersediaanDTO> rows = new ArrayList<>(loadAllRows(clientId));
            rows = filterByQuery(rows, q);
            rows = sortRows(rows, sort, dir);

            long total = rows.size();
            int from = Math.min(safePage * safeSize, rows.size());
            int to = Math.min(from + safeSize, rows.size());
            List<LaporanNilaiPersediaanDTO> content = new ArrayList<>(rows.subList(from, to));

            return ResponseEntity.ok(new LaporanNilaiPersediaanPageDTO(
                    content,
                    total,
                    safePage,
                    safeSize,
                    laporanService.getTotalAsset(clientId),
                    account.getRole().name(),
                    account.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    private List<LaporanNilaiPersediaanDTO> loadAllRows(long clientId) {
        Page<LaporanNilaiPersediaanDTO> probe =
                laporanService.getLaporanNilaiPersediaan(clientId, PageRequest.of(0, 1));
        long totalAll = probe.getTotalElements();

        if (totalAll <= 1) {
            return probe.getContent();
        }

        int fetchSize = (int) Math.min(totalAll, Integer.MAX_VALUE);
        return laporanService
                .getLaporanNilaiPersediaan(clientId, PageRequest.of(0, fetchSize))
                .getContent();
    }

    private static List<LaporanNilaiPersediaanDTO> filterByQuery(
            List<LaporanNilaiPersediaanDTO> rows,
            String q) {
        if (q == null || q.isBlank()) {
            return rows;
        }

        String needle = q.toLowerCase(Locale.ROOT).trim();
        return rows.stream()
                .filter(row -> containsIgnoreCase(row.getShortName(), needle)
                        || containsIgnoreCase(row.getFullName(), needle))
                .toList();
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static List<LaporanNilaiPersediaanDTO> sortRows(
            List<LaporanNilaiPersediaanDTO> rows,
            String sort,
            String dir) {
        if (sort == null || !SORTABLE.contains(sort)) {
            return rows;
        }

        Comparator<LaporanNilaiPersediaanDTO> comparator = switch (sort) {
            case "shortName" -> Comparator.comparing(
                    row -> nullToEmpty(row.getShortName()),
                    String.CASE_INSENSITIVE_ORDER);
            case "fullName" -> Comparator.comparing(
                    row -> nullToEmpty(row.getFullName()),
                    String.CASE_INSENSITIVE_ORDER);
            case "qty" -> Comparator.comparing(row -> nullToZero(row.getQty()));
            case "supplierPrice" -> Comparator.comparing(row -> nullToZero(row.getSupplierPrice()));
            case "hargaJual" -> Comparator.comparing(row -> nullToZero(row.getHargaJual()));
            case "totalPrice" -> Comparator.comparing(row -> nullToZero(row.getTotalPrice()));
            default -> null;
        };

        if (comparator == null) {
            return rows;
        }

        if ("desc".equalsIgnoreCase(dir)) {
            comparator = comparator.reversed();
        }

        return rows.stream().sorted(comparator).toList();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
