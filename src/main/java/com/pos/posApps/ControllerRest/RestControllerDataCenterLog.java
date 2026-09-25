package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.DataCenterLogDTO;
import com.pos.posApps.DTO.Dtos.PagedResponse;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.DataCenterLogEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.DataCenterService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/data-center")
@AllArgsConstructor
public class RestControllerDataCenterLog {
    private static final int DEFAULT_SIZE = 200;
    private static final int MAX_SIZE = 200;

    private AuthService authService;
    private DataCenterService dataCenterService;

    @GetMapping("/logs")
    public ResponseEntity<PagedResponse<DataCenterLogDTO>> list(
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

        List<DataCenterLogDTO> rows = dataCenterService.getLogData().stream()
                .map(this::toDto)
                .filter(row -> matchesSearch(row, search))
                .collect(Collectors.toCollection(ArrayList::new));
        sortRows(rows, sort, dir);

        long total = rows.size();
        int from = Math.min(safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        List<DataCenterLogDTO> content = new ArrayList<>(rows.subList(from, to));
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

    private DataCenterLogDTO toDto(DataCenterLogEntity entity) {
        return new DataCenterLogDTO(entity.getNamaFile(), entity.getCreatedAt());
    }

    private AccountEntity requireAccount(HttpSession session) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            return authService.validateToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean matchesSearch(DataCenterLogDTO row, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String needle = search.trim().toLowerCase(Locale.ROOT);
        String namaFile = row.getNamaFile() == null ? "" : row.getNamaFile().toLowerCase(Locale.ROOT);
        return namaFile.contains(needle);
    }

    private void sortRows(List<DataCenterLogDTO> rows, String sort, String dir) {
        if (sort == null || sort.isBlank()) {
            return;
        }

        Comparator<DataCenterLogDTO> comparator;
        if ("namaFile".equals(sort)) {
            comparator = Comparator.comparing(
                    DataCenterLogDTO::getNamaFile,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
        } else if ("createdAt".equals(sort)) {
            comparator = Comparator.comparing(
                    DataCenterLogDTO::getCreatedAt,
                    Comparator.nullsLast(LocalDateTime::compareTo)
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
