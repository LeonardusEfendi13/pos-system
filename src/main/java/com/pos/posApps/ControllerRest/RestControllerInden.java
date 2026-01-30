package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateIndenRequest;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.IndenService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/inden")
@AllArgsConstructor
public class RestControllerInden {
    private AuthService authService;
//    private KasirService kasirService;
    private IndenService indenService;

    @PostMapping("/add")
    public ResponseEntity<String> addInden(@RequestBody CreateIndenRequest req, HttpSession session){
        System.out.println("Req : " + req);
        AccountEntity accountData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            accountData = authService.validateToken(token);

        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        ResponseInBoolean response = indenService.createTransaction(req, accountData);
        if(response.isStatus()){
            return ResponseEntity.ok(response.getMessage());
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }

//    @PostMapping("/edit/{transactionId}")
//    public ResponseEntity<String> editTransaction(@PathVariable("transactionId") Long transactionId, @RequestBody CreateTransactionRequest req, HttpSession session){
//        AccountEntity accountData;
//        try {
//            String token = (String) session.getAttribute(authSessionKey);
//            accountData = authService.validateToken(token);
//        } catch (Exception e) {
//            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
//        }
//
//        ResponseInBoolean response = kasirService.editTransaction(transactionId, req, accountData);
//        if(response.isStatus()){
//            return ResponseEntity.ok(response.getMessage());
//        }
//        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
//    }
}
