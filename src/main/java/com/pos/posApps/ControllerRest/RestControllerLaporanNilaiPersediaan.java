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

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/laporan")
@AllArgsConstructor
public class RestControllerLaporanNilaiPersediaan {
    private static final int MAX_PAGE_SIZE = 50;

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

            Page<LaporanNilaiPersediaanDTO> result = laporanService.searchLaporanNilaiPersediaan(
                    clientId, q, sort, dir, PageRequest.of(safePage, safeSize));

            return ResponseEntity.ok(new LaporanNilaiPersediaanPageDTO(
                    result.getContent(),
                    result.getTotalElements(),
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
}
