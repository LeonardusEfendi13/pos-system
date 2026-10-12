package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CustomerDTO;
import com.pos.posApps.DTO.Dtos.CustomerLookupDTO;
import com.pos.posApps.DTO.Dtos.HutangPenjualanPageDTO;
import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.DTO.Dtos.PenjualanHistoryDetailDTO;
import com.pos.posApps.DTO.Dtos.PenjualanHistoryLineDTO;
import com.pos.posApps.DTO.Dtos.TransactionDetailDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.BranchService;
import com.pos.posApps.Service.CustomerService;
import com.pos.posApps.Service.PenjualanService;
import com.pos.posApps.Util.HutangPenjualanRange;
import com.pos.posApps.Util.PenjualanPaymentPreview;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static com.pos.posApps.Util.PenjualanHistoryCustomerFilter.cabangIdsOf;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/laporan/penjualan/hutang")
@AllArgsConstructor
public class RestControllerLaporanHutangPenjualan {
    private static final Set<String> SORTABLE = Set.of(
            "tanggalJual",
            "totalPrice",
            "customerName",
            "transactionNumber"
    );

    private AuthService authService;
    private PenjualanService penjualanService;
    private CustomerService customerService;
    private BranchService branchService;

    @GetMapping
    public ResponseEntity<HutangPenjualanPageDTO> list(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir,
            @RequestParam(required = false) String lunas) {
        HutangPenjualanRange.Bounds bounds;
        try {
            bounds = HutangPenjualanRange.resolve(startDate, endDate);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();

            int safePage = page == null || page < 0 ? 0 : page;
            int safeSize = size == null || size < 1 ? 50 : Math.min(size, 200);
            List<Long> cabangIds = cabangIdsOf(
                    branchService.getAllCabangToko().stream()
                            .filter(Objects::nonNull)
                            .map(CustomerEntity::getCustomerId)
                            .toList()
            );
            Boolean lunasFlag = parseTriState(lunas);
            PenjualanService.HutangPenjualanSlice fetched = penjualanService.getHutangPenjualanRows(
                    clientId,
                    bounds.start(),
                    bounds.end(),
                    bounds.applyStart(),
                    bounds.applyEnd(),
                    customerId,
                    cabangIds,
                    lunasFlag,
                    q,
                    PageRequest.of(safePage, safeSize, historySort(sort, dir))
            );

            var customers = customerService.getCustomerList(clientId).stream()
                    .filter(customer -> customer.getCustomerId() != null
                            && !cabangIds.contains(customer.getCustomerId()))
                    .map(this::toCustomerLookup)
                    .toList();

            return ResponseEntity.ok(new HutangPenjualanPageDTO(
                    fetched.rows().getContent(),
                    fetched.rows().getTotalElements(),
                    safePage,
                    safeSize,
                    fetched.totals().getTotalKredit(),
                    fetched.totals().getSudahDibayar(),
                    fetched.totals().getSisaHutang(),
                    customers,
                    account.getRole().name(),
                    account.getName(),
                    bounds.startIso(),
                    bounds.endIso(),
                    customerId,
                    lunasFlag
            ));
        } catch (Exception exception) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<PenjualanHistoryDetailDTO> detail(
            HttpSession session,
            @PathVariable("transactionId") Long transactionId) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            Long clientId = authService.validateToken(token).getClientEntity().getClientId();
            PenjualanDTO data = penjualanService.getHutangPenjualanDetail(clientId, transactionId);
            if (data == null) {
                return ResponseEntity.status(NOT_FOUND).build();
            }
            return ResponseEntity.ok(toDetail(data));
        } catch (Exception exception) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    private PenjualanHistoryDetailDTO toDetail(PenjualanDTO penjualan) {
        CustomerDTO customer = penjualan.getCustomerDTO();
        List<TransactionDetailDTO> details = penjualan.getTransactionDetailDTOS();
        List<PenjualanHistoryLineDTO> lines = details == null
                ? List.of()
                : details.stream().map(this::toLine).toList();

        return new PenjualanHistoryDetailDTO(
                penjualan.getTransactionId(),
                penjualan.getTransactionNumber(),
                penjualan.getTanggalJual(),
                penjualan.getTotalPrice(),
                customer == null ? "" : nullToEmpty(customer.getCustomerName()),
                nullToEmpty(penjualan.getAccountName()),
                lines,
                PenjualanPaymentPreview.lines(penjualan)
        );
    }

    private PenjualanHistoryLineDTO toLine(TransactionDetailDTO detail) {
        return new PenjualanHistoryLineDTO(
                nullToEmpty(detail.getCode()),
                nullToEmpty(detail.getName()),
                detail.getQty(),
                detail.getPrice(),
                detail.getDiscAmount(),
                detail.getTotal()
        );
    }

    private CustomerLookupDTO toCustomerLookup(CustomerEntity customer) {
        return new CustomerLookupDTO(
                customer.getCustomerId(),
                nullToEmpty(customer.getName())
        );
    }

    private static Sort historySort(String sort, String dir) {
        if (sort == null || !SORTABLE.contains(sort)) {
            return Sort.by(Sort.Direction.DESC, "transactionId");
        }

        String property = switch (sort) {
            case "tanggalJual" -> "createdAt";
            case "totalPrice" -> "totalPrice";
            case "customerName" -> "customerEntity.name";
            case "transactionNumber" -> "transactionNumber";
            default -> "transactionId";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(dir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private static Boolean parseTriState(String value) {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        return null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
