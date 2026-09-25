package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateProductRequest;
import com.pos.posApps.DTO.Dtos.EditProductRequest;
import com.pos.posApps.DTO.Dtos.KartuStokProductDTO;
import com.pos.posApps.DTO.Dtos.KartuStokResponseDTO;
import com.pos.posApps.DTO.Dtos.MasterProductLookupsDTO;
import com.pos.posApps.DTO.Dtos.PagedResponse;
import com.pos.posApps.DTO.Dtos.ProductDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.StockMovementsDTO;
import com.pos.posApps.DTO.Dtos.SupplierLookupDTO;
import com.pos.posApps.DTO.Dtos.VehicleLookupDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.CategoryService;
import com.pos.posApps.Service.ProductService;
import com.pos.posApps.Service.SupplierService;
import com.pos.posApps.Service.VehicleService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/master/product")
@AllArgsConstructor
public class RestControllerMasterProduct {
    private AuthService authService;
    private ProductService productService;
    private SupplierService supplierService;
    private VehicleService vehicleService;
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<PagedResponse<ProductDTO>> list(
            HttpSession session,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "200") Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long supplierIdFilter,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir) {
        AccountEntity account;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            account = authService.validateToken(token);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        Long clientId = account.getClientEntity().getClientId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Sort springSort = resolveSort(sort, dir);
        PageRequest pageable = springSort == null
                ? PageRequest.of(safePage, safeSize, Sort.by("fullName").ascending())
                : PageRequest.of(safePage, safeSize, springSort);
        Page<ProductDTO> productPage;
        if (search == null || search.isBlank()) {
            productPage = productService.getProductData(clientId, pageable, supplierIdFilter, false, true);
        } else {
            productPage = productService.searchProductData(clientId, search, pageable, supplierIdFilter);
        }

        return ResponseEntity.ok(new PagedResponse<>(
                productPage.getContent(),
                productPage.getTotalElements(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalPages(),
                account.getRole().name(),
                account.getName(),
                null
        ));
    }

    @GetMapping("/lookups")
    public ResponseEntity<MasterProductLookupsDTO> lookups(HttpSession session) {
        AccountEntity account;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            account = authService.validateToken(token);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        Long clientId = account.getClientEntity().getClientId();
        MasterProductLookupsDTO body = new MasterProductLookupsDTO();
        body.setRole(account.getRole().name());
        body.setAccountName(account.getName());
        body.setSuppliers(supplierService.getSupplierList(clientId).stream()
                .map(supplier -> new SupplierLookupDTO(supplier.getSupplierId(), supplier.getSupplierName()))
                .toList());
        body.setVehicles(vehicleService.getVehicleList(null).stream()
                .map(vehicle -> new VehicleLookupDTO(vehicle.getId(), vehicle.getModel(), vehicle.getBrand()))
                .toList());
        body.setCategories(categoryService.listAll(clientId));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/kartu-stok")
    public ResponseEntity<KartuStokResponseDTO> kartuStok(
            HttpSession session,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        AccountEntity account;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            account = authService.validateToken(token);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        LocalDate start = parseIsoDate(startDate, LocalDate.now().minusDays(7));
        LocalDate end = parseIsoDate(endDate, LocalDate.now());
        LocalDateTime inputStartDate = start.atStartOfDay();
        LocalDateTime inputEndDate = end.atTime(23, 59, 59);

        KartuStokResponseDTO body = emptyKartuStok(account);
        if (productId == null) {
            return ResponseEntity.ok(body);
        }

        Long clientId = account.getClientEntity().getClientId();
        ProductDTO product = productService.findProductById(productId);
        if (product.getProductId() != null) {
            body.setProduct(new KartuStokProductDTO(
                    product.getProductId(),
                    product.getShortName(),
                    product.getFullName(),
                    product.getStok()
            ));
        }

        List<StockMovementsDTO> movements = productService.getStockMovementData(
                clientId, productId, inputStartDate, inputEndDate);
        Long stockAwal = productService.getStockAwalProduct(productId, inputStartDate);
        long qtyInTotal = movements.stream()
                .mapToLong(item -> item.getQtyIn() == null ? 0L : item.getQtyIn())
                .sum();
        long qtyOutTotal = movements.stream()
                .mapToLong(item -> item.getQtyOut() == null ? 0L : item.getQtyOut())
                .sum();
        Long lastSaldo = movements.isEmpty()
                ? stockAwal
                : movements.get(movements.size() - 1).getSaldo();

        body.setMovements(movements);
        body.setStockAwal(stockAwal == null ? 0L : stockAwal);
        body.setQtyInTotal(qtyInTotal);
        body.setQtyOutTotal(qtyOutTotal);
        body.setSaldoAkhir(lastSaldo == null ? 0L : lastSaldo);
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<ResponseInBoolean> add(HttpSession session, @RequestBody CreateProductRequest req) {
        return mutate(session, () -> productService.insertProducts(req, currentClient(session)));
    }

    @PostMapping("/edit")
    public ResponseEntity<ResponseInBoolean> edit(HttpSession session, @RequestBody EditProductRequest req) {
        return mutate(session, () -> productService.editProducts(req, currentClient(session)));
    }

    @PostMapping("/delete/{productId}")
    public ResponseEntity<ResponseInBoolean> delete(HttpSession session, @PathVariable Long productId) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }
        if (!authService.hasAccessToModifyData(account.getRole())) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Anda tidak memiliki akses untuk ini!"));
        }

        boolean deleted = productService.deleteProducts(productId);
        if (deleted) {
            return ResponseEntity.ok(new ResponseInBoolean(true, "Berhasil hapus barang"));
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(new ResponseInBoolean(false, "Gagal hapus barang"));
    }

    private ResponseEntity<ResponseInBoolean> mutate(HttpSession session, Mutator mutator) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }
        if (!authService.hasAccessToModifyData(account.getRole())) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Anda tidak memiliki akses untuk ini!"));
        }

        ResponseInBoolean result = mutator.run();
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

    private ClientEntity currentClient(HttpSession session) {
        AccountEntity account = requireAccount(session);
        return account == null ? null : account.getClientEntity();
    }

    private Sort resolveSort(String sort, String dir) {
        if (sort == null || sort.isBlank()) {
            return null;
        }

        boolean desc = "desc".equalsIgnoreCase(dir);
        String property = switch (sort) {
            case "shortName" -> "shortName";
            case "fullName" -> "fullName";
            case "categoryName" -> "categoryEntity.name";
            case "minimumStock" -> "minimumStock";
            case "stok" -> "stock";
            case "hargaBeli" -> "supplierPrice";
            default -> "fullName";
        };
        return desc ? Sort.by(property).descending() : Sort.by(property).ascending();
    }

    private KartuStokResponseDTO emptyKartuStok(AccountEntity account) {
        KartuStokResponseDTO body = new KartuStokResponseDTO();
        body.setRole(account.getRole().name());
        body.setAccountName(account.getName());
        body.setProduct(null);
        body.setStockAwal(0L);
        body.setQtyInTotal(0L);
        body.setQtyOutTotal(0L);
        body.setSaldoAkhir(0L);
        body.setMovements(Collections.emptyList());
        return body;
    }

    private LocalDate parseIsoDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    @FunctionalInterface
    private interface Mutator {
        ResponseInBoolean run();
    }
}
