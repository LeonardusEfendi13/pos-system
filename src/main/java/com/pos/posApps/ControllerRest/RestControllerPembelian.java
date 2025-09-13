package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreatePurchasingRequest;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
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

    @GetMapping("/cek/{params}")
    public ResponseEntity<String> cekNoFaktur(@PathVariable("params") String params, HttpSession session){
        System.out.println("Params nya : " + params);
        String[] parts = params.split("_");
        String noFaktur = parts[0];
        Long supplierId = Long.parseLong(parts[1]);
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            System.out.println("token : " + token);
            clientData = authService.validateToken(token).getClientEntity();
        } catch (Exception e) {
            System.out.println("Exception : " + e);
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        System.out.println("otw cek");
        boolean isAvailable = pembelianService.checkNoFaktur(noFaktur, clientData, supplierId);
        if(isAvailable){
            System.out.println("Nomor faktur bisa digunakan");
            return ResponseEntity.ok("Nomor faktur bisa digunakan");
        }else{
            return ResponseEntity.ok("Nomor faktur sudah ada");
        }
    }

    @PostMapping("/add")
    public ResponseEntity<String> addTransaction(@RequestBody CreatePurchasingRequest req, HttpSession session){
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
        System.out.println(req.isCash());
        ResponseInBoolean response = pembelianService.createTransaction(req, clientData);
        if(response.isStatus()){
            System.out.println("sukses");
            return ResponseEntity.ok(response.getMessage());
        }
        System.out.println("gagal");
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }

    @PostMapping("/edit/{purchasingId}")
    public ResponseEntity<String> editTransaction(@PathVariable("purchasingId") Long purchasingId, @RequestBody CreatePurchasingRequest req, HttpSession session){
        System.out.println("purchasing Id : " + purchasingId);
        System.out.println("Edit Request received : " + req);
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            System.out.println("token : " + token);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            System.out.println("Exception : " + e);
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        System.out.println("otw edit");
        ResponseInBoolean response = pembelianService.editTransaction(purchasingId, req, clientId);
        if(response.isStatus()){
            System.out.println("sukses");
            return ResponseEntity.ok(response.getMessage());
        }
        System.out.println("gagal");
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }

    @PostMapping("/lunaskan/{selectedPembelianId}")
    public ResponseEntity<String> lunaskanPembelian(@PathVariable("selectedPembelianId") Long pembelianId, HttpSession session){
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            System.out.println("token : " + token);
            clientId = authService.validateToken(token).getClientEntity().getClientId();

            boolean isPaid = pembelianService.payFaktur(clientId, pembelianId);
            if(isPaid){
                System.out.println("Berhasil Lunaskan");
                return ResponseEntity.ok("Berhasil Bayar Faktur");
            }
            return ResponseEntity.ok("Gagal Bayar Faktur");
        } catch (Exception e) {
            System.out.println("Exception : " + e);
            return ResponseEntity.ok("Gagal Bayar Faktur");
        }
    }



}
