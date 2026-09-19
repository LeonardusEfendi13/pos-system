package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.KasirClientDTO;
import com.pos.posApps.DTO.Dtos.ProductDTO;
import com.pos.posApps.DTO.Dtos.SupplierLookupDTO;
import com.pos.posApps.DTO.Dtos.UnderstockPageDTO;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
import com.pos.posApps.Service.ProductService;
import com.pos.posApps.Service.SupplierService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/preorder")
@AllArgsConstructor
public class RestControllerPreorderUnderstock {
    private static final Set<String> SORTABLE = Set.of(
            "shortName",
            "fullName",
            "minimumStock",
            "stok",
            "hargaBeli"
    );

    private AuthService authService;
    private ProductService productService;
    private SupplierService supplierService;
    private ClientService clientService;

    @GetMapping("/understock")
    public ResponseEntity<UnderstockPageDTO> understock(
            HttpSession session,
            @RequestParam(required = false) Long supplierId,
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
            int safeSize = size == null || size < 1 ? 10 : size;

            List<ProductDTO> products = new ArrayList<>(
                    productService.getUnderstockProductData(clientId, supplierId)
            );
            products = filterByQuery(products, q);
            products = sortProducts(products, sort, dir);

            long total = products.size();
            int from = Math.min(safePage * safeSize, products.size());
            int to = Math.min(from + safeSize, products.size());
            List<ProductDTO> content = new ArrayList<>(products.subList(from, to));

            var suppliers = supplierService.getSupplierList(clientId).stream()
                    .map(supplier -> new SupplierLookupDTO(
                            supplier.getSupplierId(),
                            supplier.getSupplierName()))
                    .toList();
            var settings = clientService.getClientSettings(clientId);
            KasirClientDTO client = settings == null
                    ? new KasirClientDTO("", "", "", "", "")
                    : new KasirClientDTO(
                            settings.getName(),
                            settings.getAlamat(),
                            settings.getKota(),
                            settings.getNoTelp(),
                            settings.getCatatan() == null ? "" : settings.getCatatan());

            return ResponseEntity.ok(new UnderstockPageDTO(
                    content,
                    total,
                    safePage,
                    safeSize,
                    suppliers,
                    client,
                    account.getRole().name(),
                    account.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    private static List<ProductDTO> filterByQuery(List<ProductDTO> products, String q) {
        if (q == null || q.isBlank()) {
            return products;
        }

        String needle = q.toLowerCase(Locale.ROOT).trim();
        return products.stream()
                .filter(product -> containsIgnoreCase(product.getShortName(), needle)
                        || containsIgnoreCase(product.getFullName(), needle))
                .toList();
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static List<ProductDTO> sortProducts(
            List<ProductDTO> products,
            String sort,
            String dir) {
        if (sort == null || !SORTABLE.contains(sort)) {
            return products;
        }

        Comparator<ProductDTO> comparator = switch (sort) {
            case "shortName" -> Comparator.comparing(
                    product -> nullToEmpty(product.getShortName()),
                    String.CASE_INSENSITIVE_ORDER);
            case "fullName" -> Comparator.comparing(
                    product -> nullToEmpty(product.getFullName()),
                    String.CASE_INSENSITIVE_ORDER);
            case "minimumStock" -> Comparator.comparing(
                    product -> nullToZero(product.getMinimumStock()));
            case "stok" -> Comparator.comparing(
                    product -> nullToZero(product.getStok()));
            case "hargaBeli" -> Comparator.comparing(
                    product -> nullToZero(product.getHargaBeli()));
            default -> null;
        };

        if (comparator == null) {
            return products;
        }

        if ("desc".equalsIgnoreCase(dir)) {
            comparator = comparator.reversed();
        }

        return products.stream().sorted(comparator).toList();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
