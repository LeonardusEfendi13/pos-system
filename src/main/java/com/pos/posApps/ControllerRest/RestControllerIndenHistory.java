package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.IndenDTO;
import com.pos.posApps.DTO.Dtos.IndenHistoryPageDTO;
import com.pos.posApps.DTO.Dtos.IndenHistoryRowDTO;
import com.pos.posApps.DTO.Dtos.ResponseForWhatsapp;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.IndenService;
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
@RequestMapping("api/inden")
@AllArgsConstructor
public class RestControllerIndenHistory {
    private static final Set<String> SORTABLE = Set.of(
            "tanggalInden",
            "totalPrice",
            "custName",
            "statusInden"
    );

    private AuthService authService;
    private IndenService indenService;

    @GetMapping("/list")
    public ResponseEntity<IndenHistoryPageDTO> list(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String statusInden,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);

            int safePage = page == null || page < 0 ? 0 : page;
            int safeSize = size == null || size < 1 ? 50 : size;
            LocalDate resolvedStart = parseOrDefaultStart(startDate);
            LocalDate resolvedEnd = parseOrDefaultEnd(endDate);
            LocalDateTime inputStart = resolvedStart.atStartOfDay();
            LocalDateTime inputEnd = resolvedEnd.atTime(23, 59, 59);
            String resolvedStatus = blankToNull(statusInden);

            Page<IndenDTO> fetched;
            if (q == null || q.isBlank()) {
                fetched = indenService.getIndenData(
                        resolvedStatus,
                        inputStart,
                        inputEnd,
                        Pageable.unpaged());
            } else {
                fetched = indenService.searchIndenData(
                        resolvedStatus,
                        inputStart,
                        inputEnd,
                        q,
                        Pageable.unpaged());
            }

            List<IndenHistoryRowDTO> rows = new ArrayList<>(
                    fetched.getContent().stream().map(this::toRow).toList()
            );
            rows = sortRows(rows, sort, dir);

            int from = Math.min(safePage * safeSize, rows.size());
            int to = Math.min(from + safeSize, rows.size());
            List<IndenHistoryRowDTO> content = new ArrayList<>(rows.subList(from, to));

            return ResponseEntity.ok(new IndenHistoryPageDTO(
                    content,
                    rows.size(),
                    safePage,
                    safeSize,
                    account.getRole().name(),
                    account.getName(),
                    resolvedStart.toString(),
                    resolvedEnd.toString(),
                    resolvedStatus
            ));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @PostMapping("/delete/{indenId}")
    public ResponseEntity<ResponseInBoolean> delete(
            @PathVariable("indenId") Long indenId,
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

        boolean deleted = indenService.deleteInden(indenId);
        if (deleted) {
            return ResponseEntity.ok(new ResponseInBoolean(true, "Data berhasil dihapus"));
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(new ResponseInBoolean(false, "Gagal hapus data"));
    }

    @PostMapping("/update_status/{indenId}")
    public ResponseEntity<ResponseForWhatsapp> updateStatus(
            @PathVariable("indenId") Long indenId,
            @RequestParam String statusInden,
            HttpSession session) {
        AccountEntity account;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            account = authService.validateToken(token);
            if (account == null || account.getClientEntity() == null) {
                return ResponseEntity.status(UNAUTHORIZED)
                        .body(new ResponseForWhatsapp(false, "Harap login ulang", false, "", ""));
            }
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseForWhatsapp(false, "Harap login ulang", false, "", ""));
        }

        if (!authService.hasAccessToModifyData(account.getRole())) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseForWhatsapp(
                            false,
                            "Anda tidak memiliki akses untuk ini!",
                            false,
                            "",
                            ""));
        }

        ResponseForWhatsapp result = indenService.updateStatusInden(
                indenId,
                statusInden,
                account,
                account.getClientEntity());
        if (result.isStatus()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(result);
    }

    private IndenHistoryRowDTO toRow(IndenDTO inden) {
        return new IndenHistoryRowDTO(
                inden.getId(),
                inden.getIndenNumber(),
                inden.getTanggalInden(),
                inden.getCustName(),
                inden.getCustPhone(),
                inden.getTotalPrice(),
                inden.getDeposit(),
                inden.getSisaBayar(),
                inden.getCreatedBy(),
                inden.getStatusInden()
        );
    }

    private static List<IndenHistoryRowDTO> sortRows(
            List<IndenHistoryRowDTO> rows,
            String sort,
            String dir) {
        Comparator<IndenHistoryRowDTO> comparator;

        if (sort == null || !SORTABLE.contains(sort)) {
            comparator = Comparator.comparing(
                    IndenHistoryRowDTO::getId,
                    Comparator.nullsLast(Long::compareTo)).reversed();
        } else {
            comparator = switch (sort) {
                case "tanggalInden" -> Comparator.comparing(
                        IndenHistoryRowDTO::getTanggalInden,
                        Comparator.nullsLast(LocalDateTime::compareTo));
                case "totalPrice" -> Comparator.comparing(
                        IndenHistoryRowDTO::getTotalPrice,
                        Comparator.nullsLast(BigDecimal::compareTo));
                case "custName" -> Comparator.comparing(
                        row -> nullToEmpty(row.getCustName()),
                        String.CASE_INSENSITIVE_ORDER);
                case "statusInden" -> Comparator.comparing(
                        row -> nullToEmpty(row.getStatusInden()),
                        String.CASE_INSENSITIVE_ORDER);
                default -> Comparator.comparing(
                        IndenHistoryRowDTO::getId,
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
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
