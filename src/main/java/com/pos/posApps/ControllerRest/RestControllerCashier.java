package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateTransactionRequest;
import com.pos.posApps.DTO.Dtos.KasirBootstrapDTO;
import com.pos.posApps.DTO.Dtos.KasirClientDTO;
import com.pos.posApps.DTO.Dtos.KasirCustomerDTO;
import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
import com.pos.posApps.Service.CustomerService;
import com.pos.posApps.Service.KasirService;
import com.pos.posApps.Service.PenjualanService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/kasir")
@AllArgsConstructor
public class RestControllerCashier {
    private AuthService authService;
    private KasirService kasirService;
    private PenjualanService penjualanService;
    private CustomerService customerService;
    private ClientService clientService;

    @GetMapping("/bootstrap")
    public ResponseEntity<KasirBootstrapDTO> bootstrap(
            HttpSession session,
            @RequestParam(required = false) Long transactionId) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            var account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();
            var customers = customerService.getCustomerList(clientId).stream()
                    .map(c -> new KasirCustomerDTO(
                            c.getCustomerId(), c.getName(), c.getAlamat(), c.isKing()))
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

    @GetMapping("/transaction/list")
    public ResponseEntity<List<PenjualanDTO>> getList(HttpSession session){
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientData = authService.validateToken(token).getClientEntity();
            List<PenjualanDTO> data = penjualanService.getLast10Transaction(clientData.getClientId());
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body(Collections.emptyList());
        }
    }

    @GetMapping("/transaction/revenue")
    public ResponseEntity<BigDecimal> getRevenue(HttpSession session){
        ClientEntity clientData;
        AccountEntity accountEntity;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accountEntity = authService.validateToken(token);
            clientData = accountEntity.getClientEntity();
            BigDecimal revenue = BigDecimal.ZERO;
            if(accountEntity.getRole() == Roles.SUPER_ADMIN){
                revenue = penjualanService.getTotalRevenues(clientData.getClientId());
            }
            return ResponseEntity.ok(revenue);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body(BigDecimal.ZERO);
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PenjualanDTO> getTransaction(
            HttpSession session,
            @PathVariable Long transactionId) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            Long clientId = authService.validateToken(token).getClientEntity().getClientId();
            PenjualanDTO data = penjualanService.getPenjualanDataById(clientId, transactionId);
            if (data == null) {
                return ResponseEntity.status(NOT_FOUND).build();
            }
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }

    @PostMapping("/add")
    public ResponseEntity<String> addTransaction(@RequestBody CreateTransactionRequest req, HttpSession session){
        AccountEntity accountData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accountData = authService.validateToken(token);

        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        try {
            ResponseInBoolean response = kasirService.createTransaction(req, accountData, false);
            if (response.isStatus()) {
                return ResponseEntity.ok(response.getMessage());
            }
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    @PostMapping("/edit/{transactionId}")
    public ResponseEntity<String> editTransaction(@PathVariable("transactionId") Long transactionId, @RequestBody CreateTransactionRequest req, HttpSession session){
        AccountEntity accountData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accountData = authService.validateToken(token);
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        ResponseInBoolean response = kasirService.editTransaction(transactionId, req, accountData, false);
        if(response.isStatus()){
            return ResponseEntity.ok(response.getMessage());
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }
}
