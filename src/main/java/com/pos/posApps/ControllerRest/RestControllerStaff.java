package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.AttendanceMonthResponse;
import com.pos.posApps.DTO.Dtos.AttendanceRowDTO;
import com.pos.posApps.DTO.Dtos.InsentifPageResponse;
import com.pos.posApps.DTO.Dtos.InsentifPayoutRequest;
import com.pos.posApps.DTO.Dtos.PayrollMonthRequest;
import com.pos.posApps.DTO.Dtos.PayrollMonthResponse;
import com.pos.posApps.DTO.Dtos.PayrollRowDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.SaveAttendanceRequest;
import com.pos.posApps.DTO.Dtos.StaffDTO;
import com.pos.posApps.DTO.Dtos.StaffPageResponse;
import com.pos.posApps.DTO.Dtos.StaffWriteRequest;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.StaffEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.InsentifService;
import com.pos.posApps.Service.PayrollService;
import com.pos.posApps.Service.StaffAttendanceService;
import com.pos.posApps.Service.StaffRuleException;
import com.pos.posApps.Service.StaffService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/staff")
@AllArgsConstructor
public class RestControllerStaff {
    private static final int DEFAULT_SIZE = 200;
    private static final int MAX_SIZE = 200;

    private AuthService authService;
    private StaffService staffService;
    private StaffAttendanceService attendanceService;
    private PayrollService payrollService;
    private InsentifService insentifService;

    @GetMapping
    public ResponseEntity<StaffPageResponse> list(
            HttpSession session,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "200") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir
    ) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        Long clientId = account.getClientEntity().getClientId();
        List<StaffEntity> all = staffService.list(clientId);
        long resigned = all.stream().filter(staff -> staff.getTanggalResign() != null).count();
        BigDecimal activePayroll = all.stream()
                .map(staffService::componentTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<StaffDTO> rows = all.stream()
                .map(staffService::toDto)
                .filter(row -> matchesSearch(row, search))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        sortRows(rows, sort, dir);

        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        long total = rows.size();
        int from = Math.min(safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        return ResponseEntity.ok(new StaffPageResponse(
                new ArrayList<>(rows.subList(from, to)),
                total,
                safePage,
                safeSize,
                totalPages,
                account.getRole().name(),
                account.getName(),
                all.size(),
                all.size() - resigned,
                resigned,
                activePayroll
        ));
    }

    @GetMapping("/{staffId:\\d+}")
    public ResponseEntity<?> detail(HttpSession session, @PathVariable Long staffId) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(staffService.get(account.getClientEntity().getClientId(), staffId));
        } catch (StaffRuleException exception) {
            return rule(exception);
        }
    }

    @PostMapping
    public ResponseEntity<ResponseInBoolean> create(HttpSession session, @RequestBody StaffWriteRequest request) {
        return mutate(session, account -> {
            staffService.create(account.getClientEntity(), request);
            return new ResponseInBoolean(true, "Karyawan berhasil ditambah");
        });
    }

    @PutMapping("/{staffId:\\d+}")
    public ResponseEntity<ResponseInBoolean> update(
            HttpSession session,
            @PathVariable Long staffId,
            @RequestBody StaffWriteRequest request
    ) {
        return mutate(session, account -> {
            staffService.update(account.getClientEntity().getClientId(), staffId, request);
            return new ResponseInBoolean(true, "Karyawan berhasil diubah");
        });
    }

    @GetMapping("/attendance")
    public ResponseEntity<AttendanceMonthResponse> attendance(
            HttpSession session,
            @RequestParam Integer month,
            @RequestParam Integer year
    ) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
        try {
            YearMonth window = payrollService.window(month, year);
            List<AttendanceRowDTO> rows = attendanceService.matrix(account.getClientEntity().getClientId(), window);
            return ResponseEntity.ok(new AttendanceMonthResponse(
                    window.getMonthValue(),
                    window.getYear(),
                    window.lengthOfMonth(),
                    account.getRole().name(),
                    account.getName(),
                    rows
            ));
        } catch (StaffRuleException exception) {
            return ResponseEntity.status(exception.isNotFound() ? NOT_FOUND : BAD_REQUEST).build();
        }
    }

    @PutMapping("/attendance")
    public ResponseEntity<ResponseInBoolean> saveAttendance(
            HttpSession session,
            @RequestBody SaveAttendanceRequest request
    ) {
        return mutate(session, account -> {
            attendanceService.save(
                    account.getClientEntity().getClientId(),
                    request == null ? null : request.getItems()
            );
            return new ResponseInBoolean(true, "Absensi berhasil disimpan");
        });
    }

    @GetMapping("/payroll")
    public ResponseEntity<PayrollMonthResponse> payroll(
            HttpSession session,
            @RequestParam Integer month,
            @RequestParam Integer year
    ) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
        try {
            YearMonth window = payrollService.window(month, year);
            LocalDate today = LocalDate.now();
            List<PayrollRowDTO> rows = payrollService.rows(account.getClientEntity().getClientId(), window, today);
            return ResponseEntity.ok(new PayrollMonthResponse(
                    window.getMonthValue(),
                    window.getYear(),
                    today,
                    account.getRole().name(),
                    account.getName(),
                    rows
            ));
        } catch (StaffRuleException exception) {
            return ResponseEntity.status(BAD_REQUEST).build();
        }
    }

    @PostMapping("/payroll/pay")
    public ResponseEntity<ResponseInBoolean> payPayroll(
            HttpSession session,
            @RequestBody PayrollMonthRequest request
    ) {
        return mutate(session, account -> {
            YearMonth window = payrollWindow(request);
            payrollService.markPaid(account.getClientEntity(), request.getStaffId(), window, LocalDate.now());
            return new ResponseInBoolean(true, "Gaji ditandai sudah dibayar");
        });
    }

    @PostMapping("/payroll/unpay")
    public ResponseEntity<ResponseInBoolean> unpayPayroll(
            HttpSession session,
            @RequestBody PayrollMonthRequest request
    ) {
        return mutate(session, account -> {
            YearMonth window = payrollWindow(request);
            payrollService.cancelPaid(account.getClientEntity().getClientId(), request.getStaffId(), window);
            return new ResponseInBoolean(true, "Pembayaran gaji dibatalkan");
        });
    }

    @GetMapping("/insentif")
    public ResponseEntity<InsentifPageResponse> insentif(HttpSession session) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new InsentifPageResponse(
                account.getRole().name(),
                account.getName(),
                insentifService.rows(account.getClientEntity().getClientId())
        ));
    }

    @PostMapping("/insentif/payout")
    public ResponseEntity<ResponseInBoolean> payoutInsentif(
            HttpSession session,
            @RequestBody InsentifPayoutRequest request
    ) {
        return mutate(session, account -> {
            insentifService.cashOut(account.getClientEntity(), request == null ? null : request.getStaffId());
            return new ResponseInBoolean(true, "Insentif berhasil dicairkan");
        });
    }

    private YearMonth payrollWindow(PayrollMonthRequest request) {
        if (request == null) {
            throw new StaffRuleException("Data payroll tidak lengkap");
        }
        return payrollService.window(request.getMonth(), request.getYear());
    }

    private ResponseEntity<ResponseInBoolean> mutate(
            HttpSession session,
            java.util.function.Function<AccountEntity, ResponseInBoolean> action
    ) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }
        try {
            ResponseInBoolean result = action.apply(account);
            return ResponseEntity.ok(result);
        } catch (StaffRuleException exception) {
            return rule(exception);
        }
    }

    private ResponseEntity<ResponseInBoolean> rule(StaffRuleException exception) {
        return ResponseEntity.status(exception.isNotFound() ? NOT_FOUND : BAD_REQUEST)
                .body(new ResponseInBoolean(false, exception.getMessage()));
    }

    private AccountEntity requireAccount(HttpSession session) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            return authService.validateToken(token);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean matchesSearch(StaffDTO row, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.trim().toLowerCase(Locale.ROOT);
        return contains(row.getNama(), needle)
                || contains(row.getNik(), needle)
                || contains(row.getJabatan(), needle)
                || contains(row.getNoHp(), needle)
                || (row.getStaffId() != null && String.valueOf(row.getStaffId()).contains(needle));
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void sortRows(List<StaffDTO> rows, String sort, String dir) {
        if (sort == null || sort.isBlank()) {
            return;
        }
        boolean desc = "desc".equalsIgnoreCase(dir);
        Comparator<StaffDTO> comparator = switch (sort) {
            case "staffId" -> Comparator.comparing(StaffDTO::getStaffId, Comparator.nullsLast(Long::compareTo));
            case "jabatan" -> Comparator.comparing(row -> text(row.getJabatan()), String.CASE_INSENSITIVE_ORDER);
            case "gajiPokok" -> Comparator.comparing(StaffDTO::getGajiPokok, Comparator.nullsLast(BigDecimal::compareTo));
            case "uangMakan" -> Comparator.comparing(StaffDTO::getUangMakan, Comparator.nullsLast(BigDecimal::compareTo));
            case "insentif" -> Comparator.comparing(StaffDTO::getInsentif, Comparator.nullsLast(BigDecimal::compareTo));
            default -> Comparator.comparing(row -> text(row.getNama()), String.CASE_INSENSITIVE_ORDER);
        };
        rows.sort(desc ? comparator.reversed() : comparator);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
