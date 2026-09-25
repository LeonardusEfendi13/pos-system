package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.BranchDTO;
import com.pos.posApps.DTO.Dtos.CreateBranchRequest;
import com.pos.posApps.DTO.Dtos.PagedResponse;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.BranchService;
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
@RequestMapping("api/branch")
@AllArgsConstructor
public class RestControllerBranchDirectory {
    private static final int DEFAULT_SIZE = 200;
    private static final int MAX_SIZE = 200;

    private AuthService authService;
    private BranchService branchService;

    @GetMapping("/list")
    public ResponseEntity<PagedResponse<BranchDTO>> list(
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

        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);

        List<BranchDTO> rows = branchService.getBranchList().stream()
                .filter(row -> matchesSearch(row, search))
                .collect(Collectors.toCollection(ArrayList::new));
        sortRows(rows, sort, dir);

        long total = rows.size();
        int from = Math.min(safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        List<BranchDTO> content = new ArrayList<>(rows.subList(from, to));
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
            @RequestBody CreateBranchRequest req) {
        return mutate(session, account -> branchService.doCreateBranch(
                req == null ? null : req.getCustomerId(),
                account.getClientEntity()
        ));
    }

    @PostMapping("/delete/{branchId}")
    public ResponseEntity<ResponseInBoolean> delete(
            HttpSession session,
            @PathVariable Long branchId) {
        return mutate(session, account -> branchService.doDisableBranch(branchId));
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

    private boolean matchesSearch(BranchDTO row, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String needle = search.trim().toLowerCase(Locale.ROOT);
        String name = row.getBranchName() == null ? "" : row.getBranchName().toLowerCase(Locale.ROOT);
        if (name.contains(needle)) {
            return true;
        }

        String location = row.getLocation() == null ? "" : row.getLocation().toLowerCase(Locale.ROOT);
        return location.contains(needle);
    }

    private void sortRows(List<BranchDTO> rows, String sort, String dir) {
        if (sort == null || sort.isBlank()) {
            return;
        }

        Comparator<BranchDTO> comparator;
        if ("location".equals(sort)) {
            comparator = Comparator.comparing(
                    BranchDTO::getLocation,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
        } else if ("branchId".equals(sort)) {
            comparator = Comparator.comparing(
                    BranchDTO::getBranchId,
                    Comparator.nullsLast(Long::compareTo)
            );
        } else if ("branchName".equals(sort)) {
            comparator = Comparator.comparing(
                    BranchDTO::getBranchName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
        } else {
            return;
        }

        if ("desc".equalsIgnoreCase(dir)) {
            comparator = comparator.reversed();
        }

        rows.sort(comparator);
    }
}
