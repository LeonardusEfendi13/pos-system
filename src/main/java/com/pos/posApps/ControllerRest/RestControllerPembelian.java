package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateTransactionRequest;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.KasirService;
import com.pos.posApps.Service.PembelianService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/pembelian")
@AllArgsConstructor
public class RestControllerPembelian {
    private AuthService authService;
    private PembelianService pembelianService;

    @PostMapping("/add")
    public ResponseEntity<String> addTransaction(@RequestBody CreateTransactionRequest req, HttpSession session){
        System.out.println("Transaction received : " + req);
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            System.out.println("token : " + token);
            clientData = authService.validateToken(token).getClientEntity();
        } catch (Exception e) {
            System.out.println("Exception : " + e);
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        System.out.println("otw save");
        String isCreated = pembelianService.createTransaction(req, clientData);
        if(isCreated != null){
            System.out.println("sukses");
            return ResponseEntity.ok(isCreated);
        }
        System.out.println("gagal");
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body("Something went wrong");
    }

//    @PostMapping("/edit/{transactionId}")
//    public ResponseEntity<String> editTransaction(@PathVariable("transactionId") Long transactionId, @RequestBody CreateTransactionRequest req, HttpSession session){
//        System.out.println("transaction Id : " + transactionId);
//        System.out.println("Edit Request  received : " + req);
//        Long clientId;
//        try {
//            String token = (String) session.getAttribute(authSessionKey);
//            System.out.println("token : " + token);
//            clientId = authService.validateToken(token).getClientEntity().getClientId();
//        } catch (Exception e) {
//            System.out.println("Exception : " + e);
//            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
//        }
//
//        System.out.println("otw edit");
//        boolean isCreated = kasirService.editTransaction(transactionId, req, clientId);
//        if(isCreated){
//            System.out.println("sukses");
//            return ResponseEntity.ok("Transaction Edited");
//        }
//        System.out.println("gagal");
//        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body("Something went wrong");
//    }
}
