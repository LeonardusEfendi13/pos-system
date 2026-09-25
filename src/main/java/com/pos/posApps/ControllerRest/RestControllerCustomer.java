package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateCustomerRequest;
import com.pos.posApps.DTO.Dtos.EditCustomerRequest;
import com.pos.posApps.DTO.Dtos.KasirCustomerDTO;
import com.pos.posApps.DTO.Dtos.PagedResponse;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.CustomerService;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/customer")
@AllArgsConstructor
public class RestControllerCustomer {
    private static final int DEFAULT_SIZE = 200;
    private static final int MAX_SIZE = 200;

    private AuthService authService;
    private CustomerService customerService;

    @GetMapping("/list")
    public ResponseEntity<List<KasirCustomerDTO>> list(HttpSession session) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();
            List<KasirCustomerDTO> rows = customerService.getCustomerList(clientId)
                    .stream()
                    .map(this::toDto)
                    .toList();
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body(Collections.emptyList());
        }
    }

    @GetMapping
    public ResponseEntity<PagedResponse<KasirCustomerDTO>> pagedList(
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

        List<KasirCustomerDTO> rows = customerService.getCustomerList(clientId).stream()
                .map(this::toDto)
                .filter(row -> matchesSearch(row, search))
                .collect(Collectors.toCollection(ArrayList::new));
        sortRows(rows, sort, dir);

        long total = rows.size();
        int from = Math.min(safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        List<KasirCustomerDTO> content = new ArrayList<>(rows.subList(from, to));
        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        return ResponseEntity.ok(new PagedResponse<>(
                content,
                total,
                safePage,
                safeSize,
                totalPages,
                account.getRole().name(),
                account.getName()
        ));
    }

    @PostMapping
    public ResponseEntity<ResponseInBoolean> add(
            HttpSession session,
            @RequestBody CreateCustomerRequest req) {
        return mutate(session, account -> {
            String name = trimText(req == null ? null : req.getCustomerName());
            String alamat = trimText(req == null ? null : req.getCustomerAlamat());
            boolean isKing = req != null && Boolean.TRUE.equals(req.getIsKing());
            ClientEntity client = account.getClientEntity();
            ResponseInBoolean inserted = customerService.doCreateCustomer(name, alamat, client, isKing);
            if (inserted.isStatus()) {
                return new ResponseInBoolean(true, "Pelanggan berhasil ditambah");
            }
            if (nameExists(client.getClientId(), name, null)) {
                return new ResponseInBoolean(false, "Nama pelanggan sudah ada");
            }
            return new ResponseInBoolean(false, "Gagal menyimpan data");
        });
    }

    @PostMapping("/edit")
    public ResponseEntity<ResponseInBoolean> edit(
            HttpSession session,
            @RequestBody EditCustomerRequest req) {
        return mutate(session, account -> {
            Long customerId = req == null ? null : req.getCustomerId();
            String name = trimText(req == null ? null : req.getCustomerName());
            String alamat = trimText(req == null ? null : req.getCustomerAlamat());
            boolean isKing = req != null && Boolean.TRUE.equals(req.getIsKing());
            ClientEntity client = account.getClientEntity();
            ResponseInBoolean updated = customerService.doUpdateCustomer(
                    customerId,
                    name,
                    client.getClientId(),
                    alamat,
                    isKing
            );
            if (updated.isStatus()) {
                return new ResponseInBoolean(true, "Pelanggan berhasil diubah");
            }
            if (isMissing(customerId, client.getClientId())) {
                return new ResponseInBoolean(false, "Pelanggan tidak ditemukan");
            }
            return new ResponseInBoolean(false, "Gagal menyimpan data");
        });
    }

    @PostMapping("/delete/{customerId}")
    public ResponseEntity<ResponseInBoolean> delete(
            HttpSession session,
            @PathVariable Long customerId) {
        return mutate(session, account -> {
            ClientEntity client = account.getClientEntity();
            boolean deleted = customerService.deleteCustomer(customerId, client.getClientId());
            if (deleted) {
                return new ResponseInBoolean(true, "Pelanggan berhasil dihapus");
            }
            if (isMissing(customerId, client.getClientId())) {
                return new ResponseInBoolean(false, "Pelanggan tidak ditemukan");
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

    private KasirCustomerDTO toDto(CustomerEntity entity) {
        return new KasirCustomerDTO(
                entity.getCustomerId(),
                entity.getName(),
                entity.getAlamat(),
                entity.isKing()
        );
    }

    private boolean matchesSearch(KasirCustomerDTO row, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String needle = search.trim().toLowerCase(Locale.ROOT);
        String name = row.getName() == null ? "" : row.getName().toLowerCase(Locale.ROOT);
        if (name.contains(needle)) {
            return true;
        }

        String alamat = row.getAlamat() == null ? "" : row.getAlamat().toLowerCase(Locale.ROOT);
        if (alamat.contains(needle)) {
            return true;
        }

        String level = row.isKing() ? "king" : "biasa";
        if (level.contains(needle)) {
            return true;
        }

        return row.getCustomerId() != null && String.valueOf(row.getCustomerId()).contains(needle);
    }

    private void sortRows(List<KasirCustomerDTO> rows, String sort, String dir) {
        boolean desc = "desc".equalsIgnoreCase(dir);
        Comparator<KasirCustomerDTO> comparator;
        if ("customerId".equals(sort)) {
            comparator = Comparator.comparing(
                    KasirCustomerDTO::getCustomerId,
                    Comparator.nullsLast(Long::compareTo)
            );
        } else if ("alamat".equals(sort)) {
            comparator = Comparator.comparing(
                    KasirCustomerDTO::getAlamat,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
        } else if ("king".equals(sort)) {
            comparator = Comparator.comparing(KasirCustomerDTO::isKing);
        } else {
            comparator = Comparator.comparing(
                    KasirCustomerDTO::getName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
        }

        if (desc && sort != null && !sort.isBlank()) {
            comparator = comparator.reversed();
        }

        rows.sort(comparator);
    }

    private String trimText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isMissing(Long customerId, Long clientId) {
        if (customerId == null) {
            return true;
        }

        return customerService.getCustomerList(clientId).stream()
                .noneMatch(entity -> customerId.equals(entity.getCustomerId()));
    }

    private boolean nameExists(Long clientId, String name, Long excludeId) {
        return customerService.getCustomerList(clientId).stream()
                .anyMatch(entity -> {
                    if (excludeId != null && excludeId.equals(entity.getCustomerId())) {
                        return false;
                    }
                    String existing = entity.getName();
                    return existing != null && existing.equals(name);
                });
    }
}
