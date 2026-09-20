package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.PembelianDTO;
import com.pos.posApps.DTO.Dtos.PembelianDetailDTO;
import com.pos.posApps.DTO.Dtos.PembelianHistoryDetailDTO;
import com.pos.posApps.DTO.Dtos.PembelianHistoryLineDTO;
import com.pos.posApps.DTO.Dtos.PembelianHistoryPageDTO;
import com.pos.posApps.DTO.Dtos.PembelianHistoryRowDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.SupplierDTO;
import com.pos.posApps.DTO.Dtos.SupplierLookupDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.PembelianService;
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
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/pembelian")
@AllArgsConstructor
public class RestControllerPembelianHistory {
    private static final Set<String> SORTABLE = Set.of(
            "tanggalBeli",
            "totalPrice",
            "supplierName",
            "noFaktur"
    );

    private AuthService authService;
    private PembelianService pembelianService;
    private SupplierService supplierService;

    @GetMapping("/list")
    public ResponseEntity<PembelianHistoryPageDTO> list(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Boolean lunas,
            @RequestParam(required = false) Boolean tunai,
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

            Page<PembelianDTO> fetched;
            if (q == null || q.isBlank()) {
                fetched = pembelianService.getPembelianData(
                        account,
                        inputStart,
                        inputEnd,
                        supplierId,
                        lunas,
                        tunai,
                        Pageable.unpaged());
            } else {
                fetched = pembelianService.searchPembelianData(
                        account,
                        inputStart,
                        inputEnd,
                        supplierId,
                        lunas,
                        tunai,
                        q,
                        Pageable.unpaged());
            }

            List<PembelianHistoryRowDTO> rows = new ArrayList<>(
                    fetched.getContent().stream().map(this::toRow).toList()
            );
            rows = sortRows(rows, sort, dir);

            int from = Math.min(safePage * safeSize, rows.size());
            int to = Math.min(from + safeSize, rows.size());
            List<PembelianHistoryRowDTO> content = new ArrayList<>(rows.subList(from, to));

            BigDecimal totalPembelian = pembelianService.getTotalPurchasing(
                    clientId,
                    supplierId,
                    lunas,
                    tunai,
                    inputStart,
                    inputEnd);
            if (totalPembelian == null) {
                totalPembelian = BigDecimal.ZERO;
            }

            var suppliers = supplierService.getSupplierList(clientId).stream()
                    .map(this::toSupplierLookup)
                    .toList();

            return ResponseEntity.ok(new PembelianHistoryPageDTO(
                    content,
                    rows.size(),
                    safePage,
                    safeSize,
                    suppliers,
                    account.getRole().name(),
                    account.getName(),
                    resolvedStart.toString(),
                    resolvedEnd.toString(),
                    supplierId,
                    lunas,
                    tunai,
                    totalPembelian
            ));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @GetMapping("/{pembelianId}")
    public ResponseEntity<PembelianHistoryDetailDTO> detail(
            HttpSession session,
            @PathVariable("pembelianId") Long pembelianId) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            Long clientId = authService.validateToken(token).getClientEntity().getClientId();
            PembelianDTO data = pembelianService.getPembelianDataById(clientId, pembelianId);
            if (data == null) {
                return ResponseEntity.status(NOT_FOUND).build();
            }
            return ResponseEntity.ok(toDetail(data));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @PostMapping("/delete/{purchasingId}")
    public ResponseEntity<ResponseInBoolean> delete(
            @PathVariable("purchasingId") Long purchasingId,
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

        boolean deleted = pembelianService.deletePurchasing(
                purchasingId,
                account.getClientEntity()
        );
        if (deleted) {
            return ResponseEntity.ok(new ResponseInBoolean(true, "Data berhasil dihapus"));
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(new ResponseInBoolean(false, "Gagal hapus data"));
    }

    private PembelianHistoryRowDTO toRow(PembelianDTO pembelian) {
        SupplierDTO supplier = pembelian.getSupplierDTO();
        return new PembelianHistoryRowDTO(
                pembelian.getPembelianId(),
                nullToEmpty(pembelian.getNoFaktur()),
                pembelian.getTanggalBeli(),
                pembelian.getTanggalTempo(),
                pembelian.getTotalPrice(),
                supplier == null ? null : supplier.getSupplierId(),
                supplier == null ? "" : nullToEmpty(supplier.getSupplierName()),
                pembelian.isCash(),
                pembelian.isPaid(),
                nullToEmpty(pembelian.getAccountName())
        );
    }

    private PembelianHistoryDetailDTO toDetail(PembelianDTO pembelian) {
        SupplierDTO supplier = pembelian.getSupplierDTO();
        List<PembelianDetailDTO> details = pembelian.getPembelianDetailDTOS();
        List<PembelianHistoryLineDTO> lines = details == null
                ? List.of()
                : details.stream().map(this::toLine).toList();

        return new PembelianHistoryDetailDTO(
                pembelian.getPembelianId(),
                nullToEmpty(pembelian.getNoFaktur()),
                pembelian.getTanggalBeli(),
                pembelian.getTanggalTempo(),
                pembelian.getTotalPrice(),
                pembelian.getTotalDisc(),
                pembelian.getSubtotal(),
                supplier == null ? "" : nullToEmpty(supplier.getSupplierName()),
                pembelian.isCash(),
                pembelian.isPaid(),
                nullToEmpty(pembelian.getAccountName()),
                lines
        );
    }

    private PembelianHistoryLineDTO toLine(PembelianDetailDTO detail) {
        return new PembelianHistoryLineDTO(
                nullToEmpty(detail.getCode()),
                nullToEmpty(detail.getName()),
                detail.getQty(),
                detail.getPrice(),
                detail.getDiscAmount(),
                detail.getTotal(),
                detail.getMarkup1(),
                detail.getMarkup2(),
                detail.getMarkup3(),
                detail.getHargaJual1(),
                detail.getHargaJual2(),
                detail.getHargaJual3()
        );
    }

    private SupplierLookupDTO toSupplierLookup(SupplierEntity supplier) {
        return new SupplierLookupDTO(
                supplier.getSupplierId(),
                nullToEmpty(supplier.getSupplierName())
        );
    }

    private static List<PembelianHistoryRowDTO> sortRows(
            List<PembelianHistoryRowDTO> rows,
            String sort,
            String dir) {
        Comparator<PembelianHistoryRowDTO> comparator;

        if (sort == null || !SORTABLE.contains(sort)) {
            comparator = Comparator.comparing(
                            PembelianHistoryRowDTO::getTanggalBeli,
                            Comparator.nullsLast(LocalDateTime::compareTo))
                    .reversed()
                    .thenComparing(
                            PembelianHistoryRowDTO::getPembelianId,
                            Comparator.nullsLast(Long::compareTo).reversed());
        } else {
            comparator = switch (sort) {
                case "tanggalBeli" -> Comparator.comparing(
                        PembelianHistoryRowDTO::getTanggalBeli,
                        Comparator.nullsLast(LocalDateTime::compareTo));
                case "totalPrice" -> Comparator.comparing(
                        PembelianHistoryRowDTO::getTotalPrice,
                        Comparator.nullsLast(BigDecimal::compareTo));
                case "supplierName" -> Comparator.comparing(
                        row -> nullToEmpty(row.getSupplierName()),
                        String.CASE_INSENSITIVE_ORDER);
                case "noFaktur" -> Comparator.comparing(
                        row -> nullToEmpty(row.getNoFaktur()),
                        String.CASE_INSENSITIVE_ORDER);
                default -> Comparator.comparing(
                        PembelianHistoryRowDTO::getPembelianId,
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
