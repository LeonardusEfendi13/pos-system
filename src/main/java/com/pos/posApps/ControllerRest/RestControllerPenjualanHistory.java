package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.BuktiBayarDTO;
import com.pos.posApps.DTO.Dtos.CustomerDTO;
import com.pos.posApps.DTO.Dtos.CustomerLookupDTO;
import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.DTO.Dtos.PenjualanHistoryDetailDTO;
import com.pos.posApps.DTO.Dtos.PenjualanHistoryLineDTO;
import com.pos.posApps.DTO.Dtos.PenjualanHistoryPageDTO;
import com.pos.posApps.DTO.Dtos.PenjualanHistoryRowDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.TransactionDetailDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.BranchService;
import com.pos.posApps.Service.CustomerService;
import com.pos.posApps.Service.PenjualanService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static com.pos.posApps.Util.PenjualanHistoryCustomerFilter.cabangIdsOf;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/penjualan")
@AllArgsConstructor
public class RestControllerPenjualanHistory {
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

    @GetMapping("/list")
    public ResponseEntity<PenjualanHistoryPageDTO> list(
            HttpSession session,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String dir,
            @RequestParam(required = false) String tunai,
            @RequestParam(required = false) String lunas) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            AccountEntity account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();

            int safePage = page == null || page < 0 ? 0 : page;
            int safeSize = size == null || size < 1 ? 50 : Math.min(size, 200);
            LocalDate resolvedStart = parseOrDefaultStart(startDate);
            LocalDate resolvedEnd = parseOrDefaultEnd(endDate);
            LocalDateTime inputStart = resolvedStart.atStartOfDay();
            LocalDateTime inputEnd = resolvedEnd.atTime(23, 59, 59);
            List<Long> cabangIds = cabangIdsOf(
                    branchService.getAllCabangToko().stream()
                            .filter(Objects::nonNull)
                            .map(CustomerEntity::getCustomerId)
                            .toList()
            );

            Boolean tunaiFlag = parseTriState(tunai);
            Boolean lunasFlag = parseTriState(lunas);
            Page<PenjualanHistoryRowDTO> fetched = penjualanService.getPenjualanHistoryRows(
                    clientId,
                    inputStart,
                    inputEnd,
                    customerId,
                    cabangIds,
                    tunaiFlag,
                    lunasFlag,
                    q,
                    PageRequest.of(safePage, safeSize, historySort(sort, dir))
            );

            var customers = customerService.getCustomerList(clientId).stream()
                    .filter(customer -> customer.getCustomerId() != null
                            && !cabangIds.contains(customer.getCustomerId()))
                    .map(this::toCustomerLookup)
                    .toList();

            return ResponseEntity.ok(new PenjualanHistoryPageDTO(
                    fetched.getContent(),
                    fetched.getTotalElements(),
                    safePage,
                    safeSize,
                    customers,
                    account.getRole().name(),
                    account.getName(),
                    resolvedStart.toString(),
                    resolvedEnd.toString(),
                    customerId,
                    tunaiFlag,
                    lunasFlag
            ));
        } catch (Exception e) {
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
            PenjualanDTO data = penjualanService.getPenjualanDataById(clientId, transactionId);
            if (data == null) {
                return ResponseEntity.status(NOT_FOUND).build();
            }
            return ResponseEntity.ok(toDetail(data));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @PostMapping("/delete/{transactionId}")
    public ResponseEntity<ResponseInBoolean> delete(
            @PathVariable("transactionId") Long transactionId,
            HttpSession session) {
        AccountEntity account;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            account = authService.validateToken(token);
            if (account == null || account.getClientEntity() == null) {
                return ResponseEntity.status(UNAUTHORIZED)
                        .body(new ResponseInBoolean(false, "Harap login ulang"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }

        if (!authService.hasAccessToModifyData(account.getRole())) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Anda tidak memiliki akses untuk ini!"));
        }

        boolean deleted = penjualanService.deletePenjualan(
                transactionId,
                account.getClientEntity()
        );
        if (deleted) {
            return ResponseEntity.ok(new ResponseInBoolean(true, "Data berhasil dihapus"));
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(new ResponseInBoolean(false, "Gagal hapus data"));
    }

    @PostMapping("/lunaskan")
    public ResponseEntity<ResponseInBoolean> lunaskan(
            @RequestParam Long transactionId,
            @RequestParam Long paymentMethodId,
            @RequestPart(required = false) MultipartFile buktiPembayaran,
            HttpSession session
    ) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            Long clientId = authService.validateToken(token).getClientEntity().getClientId();
            return ResponseEntity.ok(penjualanService.payFaktur(
                    clientId,
                    transactionId,
                    paymentMethodId,
                    buktiPembayaran
            ));
        } catch (Exception exception) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }
    }

    @GetMapping("/bukti/{transactionId}")
    public ResponseEntity<BuktiBayarDTO> bukti(
            @PathVariable("transactionId") Long transactionId,
            HttpSession session
    ) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            authService.validateToken(token);
            return ResponseEntity.ok(penjualanService.getBuktiPembayaran(transactionId));
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
                lines
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

    private static LocalDate parseOrDefaultStart(String startDate) {
        if (startDate == null || startDate.isBlank()) {
            return LocalDate.now().withDayOfMonth(1);
        }
        try {
            return LocalDate.parse(startDate);
        } catch (Exception e) {
            return LocalDate.now().withDayOfMonth(1);
        }
    }

    private static LocalDate parseOrDefaultEnd(String endDate) {
        if (endDate == null || endDate.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(endDate);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
