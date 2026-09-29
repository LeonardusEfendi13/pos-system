package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.PagedResponse;
import com.pos.posApps.DTO.Dtos.ProductSetDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.SaveProductSetRequest;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ProductSetService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Function;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/product-set")
@AllArgsConstructor
public class RestControllerProductSet {
    private AuthService authService;
    private ProductSetService productSetService;

    @GetMapping
    public ResponseEntity<PagedResponse<ProductSetDTO>> list(
            HttpSession session,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "200") Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir
    ) {
        AccountEntity account;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            account = authService.validateToken(token);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Sort springSort = resolveSort(sort, dir);
        PageRequest pageable = springSort == null
                ? PageRequest.of(safePage, safeSize, Sort.by("parentProduct.fullName").ascending())
                : PageRequest.of(safePage, safeSize, springSort);
        Page<ProductSetDTO> result = productSetService.search(
                account.getClientEntity().getClientId(),
                search,
                pageable
        );

        return ResponseEntity.ok(new PagedResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages(),
                account.getRole().name(),
                account.getName(),
                null
        ));
    }

    @GetMapping("/by-parent/{productId}")
    public ResponseEntity<?> byParent(HttpSession session, @PathVariable Long productId) {
        AccountEntity account;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            account = authService.validateToken(token);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }

        return productSetService
                .findByParent(account.getClientEntity().getClientId(), productId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(NOT_FOUND)
                        .body(new ResponseInBoolean(false, "Group set tidak ditemukan")));
    }

    @PostMapping
    public ResponseEntity<ResponseInBoolean> add(HttpSession session, @RequestBody SaveProductSetRequest req) {
        return mutate(session, account -> productSetService.save(req, account.getClientEntity()));
    }

    @PostMapping("/edit")
    public ResponseEntity<ResponseInBoolean> edit(HttpSession session, @RequestBody SaveProductSetRequest req) {
        return mutate(session, account -> productSetService.save(req, account.getClientEntity()));
    }

    @PostMapping("/delete/{productSetId}")
    public ResponseEntity<ResponseInBoolean> delete(HttpSession session, @PathVariable Long productSetId) {
        return mutate(session, account -> productSetService.delete(productSetId, account.getClientEntity()));
    }

    private ResponseEntity<ResponseInBoolean> mutate(
            HttpSession session,
            Function<AccountEntity, ResponseInBoolean> action
    ) {
        AccountEntity account;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            account = authService.validateToken(token);
        } catch (Exception e) {
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

    private Sort resolveSort(String sort, String dir) {
        if (sort == null || sort.isBlank()) {
            return null;
        }

        boolean desc = "desc".equalsIgnoreCase(dir);
        String property = switch (sort) {
            case "parentShortName" -> "parentProduct.shortName";
            case "parentFullName" -> "parentProduct.fullName";
            default -> "parentProduct.fullName";
        };
        return desc ? Sort.by(property).descending() : Sort.by(property).ascending();
    }
}
