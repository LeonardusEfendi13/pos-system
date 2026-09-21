package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.KasirBootstrapDTO;
import com.pos.posApps.DTO.Dtos.KasirClientDTO;
import com.pos.posApps.DTO.Dtos.KasirCustomerDTO;
import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.BranchService;
import com.pos.posApps.Service.ClientService;
import com.pos.posApps.Service.PenjualanService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/branch/kasir")
@AllArgsConstructor
public class RestControllerBranchBootstrap {
    private AuthService authService;
    private BranchService branchService;
    private ClientService clientService;
    private PenjualanService penjualanService;

    @GetMapping("/bootstrap")
    public ResponseEntity<KasirBootstrapDTO> bootstrap(
            HttpSession session,
            @RequestParam(required = false) Long transactionId) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            var account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();
            var customers = branchService.getAllCabangToko().stream()
                    .filter(Objects::nonNull)
                    .map(this::toCustomerDto)
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
            PenjualanDTO transaction = null;
            if (transactionId != null) {
                transaction = penjualanService.getPenjualanDataById(clientId, transactionId);
                if (transaction != null && !isCabangTransaction(transaction, customers)) {
                    transaction = null;
                }
            }
            return ResponseEntity.ok(
                    new KasirBootstrapDTO(
                            customers,
                            client,
                            transaction,
                            account.getRole().name()));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    private KasirCustomerDTO toCustomerDto(CustomerEntity customer) {
        return new KasirCustomerDTO(
                customer.getCustomerId(),
                customer.getName(),
                customer.getAlamat() == null ? "" : customer.getAlamat(),
                customer.isKing());
    }

    private boolean isCabangTransaction(
            PenjualanDTO transaction,
            List<KasirCustomerDTO> customers) {
        if (transaction.getCustomerDTO() == null || transaction.getCustomerDTO().getCustomerId() == null) {
            return false;
        }

        Set<Long> cabangIds = customers.stream()
                .map(KasirCustomerDTO::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return cabangIds.contains(transaction.getCustomerDTO().getCustomerId());
    }
}
