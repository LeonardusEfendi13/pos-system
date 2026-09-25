package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateSupplierRequest;
import com.pos.posApps.DTO.Dtos.EditSupplierRequest;
import com.pos.posApps.DTO.Dtos.PagedResponse;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.SupplierDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.SupplierService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/supplier")
@AllArgsConstructor
public class RestControllerSupplier {
    private static final int DEFAULT_SIZE = 200;
    private static final int MAX_SIZE = 200;

    private AuthService authService;
    private SupplierService supplierService;

    @GetMapping
    public ResponseEntity<PagedResponse<SupplierDTO>> list(
            HttpSession session,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "200") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        Long clientId = account.getClientEntity().getClientId();
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);

        List<SupplierDTO> rows = supplierService.getSupplierList(clientId).stream()
                .map(this::toDto)
                .filter(row -> matchesSearch(row, search))
                .collect(Collectors.toCollection(ArrayList::new));
        sortRows(rows, sort, dir);

        long total = rows.size();
        int from = Math.min(safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        List<SupplierDTO> content = new ArrayList<>(rows.subList(from, to));
        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        return ResponseEntity.ok(new PagedResponse<>(
                content,
                total,
                safePage,
                safeSize,
                totalPages,
                account.getRole().name(),
                account.getName(),
                null
        ));
    }

    @PostMapping
    public ResponseEntity<ResponseInBoolean> add(
            HttpSession session,
            @RequestBody CreateSupplierRequest req) {
        return mutate(session, account -> {
            String name = trimName(req == null ? null : req.getSupplierName());
            ClientEntity client = account.getClientEntity();
            boolean inserted = supplierService.insertSupplier(name, client);
            if (inserted) {
                return new ResponseInBoolean(true, "Supplier berhasil ditambah");
            }
            if (nameExists(client.getClientId(), name, null)) {
                return new ResponseInBoolean(false, "Nama supplier sudah ada");
            }
            return new ResponseInBoolean(false, "Gagal menyimpan data");
        });
    }

    @PostMapping("/edit")
    public ResponseEntity<ResponseInBoolean> edit(
            HttpSession session,
            @RequestBody EditSupplierRequest req) {
        return mutate(session, account -> {
            Long supplierId = req == null ? null : req.getSupplierId();
            String name = trimName(req == null ? null : req.getSupplierName());
            ClientEntity client = account.getClientEntity();
            boolean edited = supplierService.editSupplier(supplierId, name, client);
            if (edited) {
                return new ResponseInBoolean(true, "Supplier berhasil diubah");
            }
            if (isMissing(supplierId, client.getClientId())) {
                return new ResponseInBoolean(false, "Supplier tidak ditemukan");
            }
            if (nameExists(client.getClientId(), name, supplierId)) {
                return new ResponseInBoolean(false, "Nama supplier sudah ada");
            }
            return new ResponseInBoolean(false, "Gagal menyimpan data");
        });
    }

    @PostMapping("/delete/{supplierId}")
    public ResponseEntity<ResponseInBoolean> delete(
            HttpSession session,
            @PathVariable Long supplierId) {
        return mutate(session, account -> {
            ClientEntity client = account.getClientEntity();
            boolean deleted = supplierService.disableSupplier(supplierId, client);
            if (deleted) {
                return new ResponseInBoolean(true, "Supplier berhasil dihapus");
            }
            if (isMissing(supplierId, client.getClientId())) {
                return new ResponseInBoolean(false, "Supplier tidak ditemukan");
            }
            return new ResponseInBoolean(false, "Gagal menyimpan data");
        });
    }

    private ResponseEntity<ResponseInBoolean> mutate(
            HttpSession session,
            Function<AccountEntity, ResponseInBoolean> action
    ) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }
        if (!authService.hasAccessToModifyData(account.getRole())) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Anda tidak memiliki akses untuk ini!"));
        }

        ResponseInBoolean result = action.apply(account);
        if (result.isStatus()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(result);
    }

    private AccountEntity requireAccount(HttpSession session) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            return authService.validateToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private SupplierDTO toDto(SupplierEntity entity) {
        return new SupplierDTO(entity.getSupplierId(), entity.getSupplierName());
    }

    private boolean matchesSearch(SupplierDTO row, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String needle = search.trim().toLowerCase(Locale.ROOT);
        String name = row.getSupplierName() == null ? "" : row.getSupplierName().toLowerCase(Locale.ROOT);
        if (name.contains(needle)) {
            return true;
        }

        return row.getSupplierId() != null && String.valueOf(row.getSupplierId()).contains(needle);
    }

    private void sortRows(List<SupplierDTO> rows, String sort, String dir) {
        boolean desc = "desc".equalsIgnoreCase(dir);
        Comparator<SupplierDTO> comparator;
        if ("supplierId".equals(sort)) {
            comparator = Comparator.comparing(
                    SupplierDTO::getSupplierId,
                    Comparator.nullsLast(Long::compareTo)
            );
        } else {
            comparator = Comparator.comparing(
                    SupplierDTO::getSupplierName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
        }

        if (desc && sort != null && !sort.isBlank()) {
            comparator = comparator.reversed();
        }

        rows.sort(comparator);
    }

    private String trimName(String supplierName) {
        return supplierName == null ? "" : supplierName.trim();
    }

    private boolean isMissing(Long supplierId, Long clientId) {
        if (supplierId == null) {
            return true;
        }

        return "INVALID".equals(supplierService.getSupplierDataById(supplierId, clientId));
    }

    private boolean nameExists(Long clientId, String name, Long excludeId) {
        return supplierService.getSupplierList(clientId).stream()
                .anyMatch(entity -> {
                    if (excludeId != null && excludeId.equals(entity.getSupplierId())) {
                        return false;
                    }
                    String existing = entity.getSupplierName();
                    return existing != null && existing.equalsIgnoreCase(name);
                });
    }
}
