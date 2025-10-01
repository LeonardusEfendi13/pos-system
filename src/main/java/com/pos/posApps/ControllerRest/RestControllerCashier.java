package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateTransactionRequest;
import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
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
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/kasir")
@AllArgsConstructor
public class RestControllerCashier {
    private AuthService authService;
    private KasirService kasirService;
    private PenjualanService penjualanService;

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
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientData = authService.validateToken(token).getClientEntity();
            BigDecimal revenue = penjualanService.getTotalRevenues(clientData.getClientId());
            return ResponseEntity.ok(revenue);

        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body(BigDecimal.ZERO);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<String> addTransaction(@RequestBody CreateTransactionRequest req, HttpSession session){
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientData = authService.validateToken(token).getClientEntity();
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        ResponseInBoolean response = kasirService.createTransaction(req, clientData);
        if(response.isStatus()){
            return ResponseEntity.ok(response.getMessage());
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }

    @PostMapping("/edit/{transactionId}")
    public ResponseEntity<String> editTransaction(@PathVariable("transactionId") Long transactionId, @RequestBody CreateTransactionRequest req, HttpSession session){
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientData = authService.validateToken(token).getClientEntity();
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        ResponseInBoolean response = kasirService.editTransaction(transactionId, req, clientData);
        if(response.isStatus()){
            return ResponseEntity.ok(response.getMessage());
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }
}
