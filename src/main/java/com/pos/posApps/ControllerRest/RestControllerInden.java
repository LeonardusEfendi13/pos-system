package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateTransactionRequest;
import com.pos.posApps.DTO.Dtos.PenjualanDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.AccountEntity;
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
@RequestMapping("api/inden")
@AllArgsConstructor
public class RestControllerInden {
    private AuthService authService;
    private KasirService kasirService;

    @PostMapping("/add")
    public ResponseEntity<String> addTransaction(@RequestBody CreateTransactionRequest req, HttpSession session){
        AccountEntity accountData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accountData = authService.validateToken(token);

        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        ResponseInBoolean response = kasirService.createTransaction(req, accountData);
        if(response.isStatus()){
            return ResponseEntity.ok(response.getMessage());
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
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

        ResponseInBoolean response = kasirService.editTransaction(transactionId, req, accountData);
        if(response.isStatus()){
            return ResponseEntity.ok(response.getMessage());
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }
}
