package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateUserRequest;
import com.pos.posApps.DTO.Dtos.EditUserRequest;
import com.pos.posApps.DTO.Dtos.PagedResponse;
import com.pos.posApps.DTO.Dtos.RegisterRequest;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.UserDTO;
import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Repository.AccountRepository;
import com.pos.posApps.Service.AccountService;
import com.pos.posApps.Service.AuthService;
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
@RequestMapping("api/user")
@AllArgsConstructor
public class RestControllerUser {
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 50;

    private AuthService authService;
    private AccountService accountService;
    private AccountRepository accountRepository;

    @GetMapping
    public ResponseEntity<PagedResponse<UserDTO>> list(
            HttpSession session,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        Long clientId = account.getClientEntity().getClientId();
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);

        List<UserDTO> rows = accountService.getUserList(clientId).stream()
                .filter(row -> matchesSearch(row, search))
                .collect(Collectors.toCollection(ArrayList::new));
        sortRows(rows, sort, dir);

        long total = rows.size();
        int from = Math.min(safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        List<UserDTO> content = new ArrayList<>(rows.subList(from, to));
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
            @RequestBody CreateUserRequest req) {
        return mutate(session, account -> {
            String name = trimText(req == null ? null : req.getName());
            String username = trimText(req == null ? null : req.getUsername());
            String password = trimText(req == null ? null : req.getPassword());
            Roles role = parseAssignableRole(req == null ? null : req.getRole());
            if (role == null) {
                return new ResponseInBoolean(false, "Role tidak valid");
            }
            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                return new ResponseInBoolean(false, "Gagal menyimpan data");
            }
            if (usernameExists(username, null)) {
                return new ResponseInBoolean(false, "Username sudah ada");
            }

            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setName(name);
            registerRequest.setUsername(username);
            registerRequest.setPassword(password);
            registerRequest.setRole(role);
            boolean inserted = accountService.doCreateAccount(registerRequest, account.getClientEntity());
            if (inserted) {
                return new ResponseInBoolean(true, "User berhasil ditambah");
            }
            if (usernameExists(username, null)) {
                return new ResponseInBoolean(false, "Username sudah ada");
            }
            return new ResponseInBoolean(false, "Gagal menyimpan data");
        });
    }

    @PostMapping("/edit")
    public ResponseEntity<ResponseInBoolean> edit(
            HttpSession session,
            @RequestBody EditUserRequest req) {
        return mutate(session, account -> {
            Long userId = req == null ? null : req.getId();
            String name = trimText(req == null ? null : req.getName());
            String username = trimText(req == null ? null : req.getUsername());
            Roles role = req == null ? null : req.getRole();
            ClientEntity client = account.getClientEntity();
            if (!isAssignableRole(role)) {
                return new ResponseInBoolean(false, "Role tidak valid");
            }
            if (name.isEmpty() || username.isEmpty()) {
                return new ResponseInBoolean(false, "Gagal menyimpan data");
            }
            if (findOwned(userId, client.getClientId()) == null) {
                return new ResponseInBoolean(false, "User tidak ditemukan");
            }
            if (usernameExists(username, userId)) {
                return new ResponseInBoolean(false, "Username sudah ada");
            }

            EditUserRequest update = new EditUserRequest();
            update.setId(userId);
            update.setName(name);
            update.setUsername(username);
            update.setRole(role);
            boolean updated = accountService.doUpdateAccount(update);
            if (updated) {
                return new ResponseInBoolean(true, "User berhasil diubah");
            }
            return new ResponseInBoolean(false, "Gagal menyimpan data");
        });
    }

    @PostMapping("/delete/{userId}")
    public ResponseEntity<ResponseInBoolean> delete(
            HttpSession session,
            @PathVariable Long userId) {
        return mutate(session, account -> {
            AccountEntity target = findOwned(userId, account.getClientEntity().getClientId());
            if (target == null) {
                return new ResponseInBoolean(false, "User tidak ditemukan");
            }
            if (target.getRole() == Roles.SUPER_ADMIN || target.getRole() == Roles.GOD_ADMIN) {
                return new ResponseInBoolean(false, "User Super Admin tidak dapat dihapus");
            }

            boolean deleted = accountService.doDisableAccount(userId);
            if (deleted) {
                return new ResponseInBoolean(true, "User berhasil dihapus");
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

    private boolean matchesSearch(UserDTO row, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String needle = search.trim().toLowerCase(Locale.ROOT);
        String name = row.getName() == null ? "" : row.getName().toLowerCase(Locale.ROOT);
        if (name.contains(needle)) {
            return true;
        }

        String username = row.getUsername() == null ? "" : row.getUsername().toLowerCase(Locale.ROOT);
        if (username.contains(needle)) {
            return true;
        }

        if (roleSearchHaystack(row.getRole()).contains(needle)) {
            return true;
        }

        return row.getUserId() != null && String.valueOf(row.getUserId()).contains(needle);
    }

    private String roleSearchHaystack(Roles role) {
        if (role == null) {
            return "";
        }

        String enumName = role.name().toLowerCase(Locale.ROOT);
        String spaced = enumName.replace('_', ' ');
        return enumName + " " + spaced;
    }

    private void sortRows(List<UserDTO> rows, String sort, String dir) {
        boolean knownTextSort = "name".equals(sort) || "username".equals(sort) || "role".equals(sort);
        boolean desc = knownTextSort
                ? "desc".equalsIgnoreCase(dir)
                : !"asc".equalsIgnoreCase(dir);

        Comparator<UserDTO> comparator;
        if ("name".equals(sort)) {
            comparator = Comparator.comparing(
                    UserDTO::getName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
        } else if ("username".equals(sort)) {
            comparator = Comparator.comparing(
                    UserDTO::getUsername,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
        } else if ("role".equals(sort)) {
            comparator = Comparator.comparing(
                    row -> row.getRole() == null ? "" : row.getRole().name(),
                    String.CASE_INSENSITIVE_ORDER
            );
        } else {
            comparator = Comparator.comparing(
                    UserDTO::getUserId,
                    Comparator.nullsLast(Long::compareTo)
            );
        }

        if (desc) {
            comparator = comparator.reversed();
        }

        rows.sort(comparator);
    }

    private String trimText(String value) {
        return value == null ? "" : value.trim();
    }

    private Roles parseAssignableRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return toAssignableRole(Roles.valueOf(raw.trim()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isAssignableRole(Roles role) {
        return toAssignableRole(role) != null;
    }

    private Roles toAssignableRole(Roles role) {
        if (role == null || role == Roles.GOD_ADMIN) {
            return null;
        }
        return role;
    }

    private boolean usernameExists(String username, Long excludeId) {
        AccountEntity existing = accountRepository.findByUsernameAndDeletedAtIsNull(username);
        if (existing == null) {
            return false;
        }
        return excludeId == null || !excludeId.equals(existing.getAccountId());
    }

    private AccountEntity findOwned(Long userId, Long clientId) {
        if (userId == null || clientId == null) {
            return null;
        }

        AccountEntity entity = accountRepository.findByAccountIdAndDeletedAtIsNull(userId);
        if (entity == null || entity.getClientEntity() == null) {
            return null;
        }
        if (!clientId.equals(entity.getClientEntity().getClientId())) {
            return null;
        }
        return entity;
    }
}
