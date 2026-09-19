package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.PreorderHistoryPageDTO;
import com.pos.posApps.DTO.Dtos.PreorderHistoryRowDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.SupplierLookupDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.PreorderEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.PreorderService;
import com.pos.posApps.Service.SupplierService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/preorder")
@AllArgsConstructor
public class RestControllerPreorderHistory {
    private static final Set<String> SORTABLE = Set.of(
            "createdAt",
            "totalPrice",
            "supplierName"
    );

    private AuthService authService;
    private PreorderService preorderService;
    private SupplierService supplierService;

    @GetMapping("/list")
    public ResponseEntity<PreorderHistoryPageDTO> list(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();

            int safePage = page == null || page < 0 ? 0 : page;
            int safeSize = size == null || size < 1 ? 50 : size;
            LocalDate resolvedStart = parseOrDefaultStart(startDate);
            LocalDate resolvedEnd = parseOrDefaultEnd(endDate);
            LocalDateTime inputStart = resolvedStart.atStartOfDay();
            LocalDateTime inputEnd = resolvedEnd.atTime(23, 59, 59);

            Page<PreorderEntity> fetched;
            if (q == null || q.isBlank()) {
                fetched = preorderService.getPreorderData(
                        clientId,
                        supplierId,
                        inputStart,
                        inputEnd,
                        Pageable.unpaged());
            } else {
                fetched = preorderService.searchPreorderData(
                        clientId,
                        supplierId,
                        inputStart,
                        inputEnd,
                        q,
                        Pageable.unpaged());
            }

            List<PreorderHistoryRowDTO> rows = new ArrayList<>(
                    fetched.getContent().stream().map(this::toRow).toList()
            );
            rows = sortRows(rows, sort, dir);

            long total = rows.size();
            int from = Math.min(safePage * safeSize, rows.size());
            int to = Math.min(from + safeSize, rows.size());
            List<PreorderHistoryRowDTO> content = new ArrayList<>(rows.subList(from, to));

            var suppliers = supplierService.getSupplierList(clientId).stream()
                    .map(supplier -> new SupplierLookupDTO(
                            supplier.getSupplierId(),
                            supplier.getSupplierName()))
                    .toList();

            return ResponseEntity.ok(new PreorderHistoryPageDTO(
                    content,
                    total,
                    safePage,
                    safeSize,
                    suppliers,
                    account.getRole().name(),
                    account.getName(),
                    resolvedStart.toString(),
                    resolvedEnd.toString(),
                    supplierId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @PostMapping("/delete/{preorderId}")
    public ResponseEntity<ResponseInBoolean> delete(
            @PathVariable("preorderId") Long preorderId,
            HttpSession session) {
        AccountEntity account;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            account = authService.validateToken(token);
            if (account == null || account.getClientEntity() == null) {
                return ResponseEntity.status(UNAUTHORIZED)
                        .body(new ResponseInBoolean(false, "Harap login ulang"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }

        if (!authService.hasAccessToModifyData(account.getRole())) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Anda tidak memiliki akses untuk ini!"));
        }

        boolean deleted = preorderService.deletePreorder(
                preorderId,
                account.getClientEntity()
        );
        if (deleted) {
            return ResponseEntity.ok(new ResponseInBoolean(true, "Data berhasil dihapus"));
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(new ResponseInBoolean(false, "Gagal hapus data"));
    }

    private PreorderHistoryRowDTO toRow(PreorderEntity preorder) {
        SupplierEntity supplier = preorder.getSupplierEntity();
        return new PreorderHistoryRowDTO(
                preorder.getPreorderId(),
                preorder.getCreatedAt(),
                preorder.getTotalPrice(),
                supplier == null ? null : supplier.getSupplierId(),
                supplier == null ? "" : supplier.getSupplierName()
        );
    }

    private static List<PreorderHistoryRowDTO> sortRows(
            List<PreorderHistoryRowDTO> rows,
            String sort,
            String dir) {
        Comparator<PreorderHistoryRowDTO> comparator;

        if (sort == null || !SORTABLE.contains(sort)) {
            comparator = Comparator.comparing(
                    PreorderHistoryRowDTO::getPreorderId,
                    Comparator.nullsLast(Long::compareTo)).reversed();
        } else {
            comparator = switch (sort) {
                case "createdAt" -> Comparator.comparing(
                        PreorderHistoryRowDTO::getCreatedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo));
                case "totalPrice" -> Comparator.comparing(
                        PreorderHistoryRowDTO::getTotalPrice,
                        Comparator.nullsLast(BigDecimal::compareTo));
                case "supplierName" -> Comparator.comparing(
                        row -> nullToEmpty(row.getSupplierName()),
                        String.CASE_INSENSITIVE_ORDER);
                default -> Comparator.comparing(
                        PreorderHistoryRowDTO::getPreorderId,
                        Comparator.nullsLast(Long::compareTo)).reversed();
            };

            if ("desc".equalsIgnoreCase(dir)) {
                comparator = comparator.reversed();
            }
        }

        return rows.stream().sorted(comparator).toList();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static LocalDate parseOrDefaultStart(String startDate) {
        if (startDate == null || startDate.isBlank()) {
            return LocalDate.now().withDayOfMonth(1);
        }
        try {
            return LocalDate.parse(startDate);
        } catch (Exception e) {
            return LocalDate.now().withDayOfMonth(1);
        }
    }

    private static LocalDate parseOrDefaultEnd(String endDate) {
        if (endDate == null || endDate.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(endDate);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
