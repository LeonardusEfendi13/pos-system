package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.BuktiBayarDTO;
import com.pos.posApps.DTO.Dtos.CreatePurchasingRequest;
import com.pos.posApps.DTO.Dtos.LunaskanPembelianDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.PembelianService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    public ResponseEntity<String> cekNoFaktur(@PathVariable("params") String params, HttpSession session) {
        String[] parts = params.split("_");
        String noFaktur = parts[0];
        Long supplierId = Long.parseLong(parts[1]);
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientData = authService.validateToken(token).getClientEntity();
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        boolean isAvailable = pembelianService.checkNoFaktur(noFaktur, clientData, supplierId);
        if (isAvailable) {
            return ResponseEntity.ok("Nomor faktur bisa digunakan");
        } else {
            return ResponseEntity.ok("Nomor faktur sudah ada");
        }
    }

    @PostMapping("/add")
    public ResponseEntity<String> addTransaction(@RequestBody CreatePurchasingRequest req, HttpSession session) {
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientData = authService.validateToken(token).getClientEntity();
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }
        ResponseInBoolean response = pembelianService.createTransaction(req, clientData);
        if (response.isStatus()) {
            return ResponseEntity.ok(response.getMessage());
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }

    @PostMapping("/edit/{purchasingId}")
    public ResponseEntity<String> editTransaction(@PathVariable("purchasingId") Long purchasingId, @RequestBody CreatePurchasingRequest req, HttpSession session) {
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientData = authService.validateToken(token).getClientEntity();
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }
        ResponseInBoolean response = pembelianService.editTransaction(purchasingId, req, clientData);
        if (response.isStatus()) {
            return ResponseEntity.ok(response.getMessage());
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }

    @PostMapping("/lunaskan")
    public ResponseEntity<String> lunaskanPembelian(
            @RequestParam Long pembelianId,
            @RequestParam String jenisPembayaran,
            @RequestParam(required = false) String rekeningAsal,
            @RequestParam(required = false) String rekeningTujuan,
            @RequestPart(required = false) MultipartFile buktiPembayaran,
            HttpSession session
    ) {
        LunaskanPembelianDTO req = new LunaskanPembelianDTO();
        req.setPembelianId(pembelianId);
        req.setJenisPembayaran(jenisPembayaran);
        req.setRekeningAsal(rekeningAsal);
        req.setRekeningTujuan(rekeningTujuan);
        req.setBuktiPembayaran(buktiPembayaran);
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
            ResponseInBoolean isPaid = pembelianService.payFaktur(clientId, req);
            return ResponseEntity.ok(isPaid.getMessage());
        } catch (Exception e) {
            return ResponseEntity.ok("Gagal Bayar Faktur");
        }
    }

    @GetMapping("/bukti/{pembelianId}")
    public ResponseEntity<BuktiBayarDTO> getBuktiPembelian(
            @PathVariable("pembelianId") Long pembelianId,
            HttpSession session
    ){
        try {
            String token = (String) session.getAttribute(authSessionKey);
            authService.validateToken(token);
            BuktiBayarDTO data = pembelianService.getBuktiPembayaran(pembelianId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.ok(new BuktiBayarDTO());
        }
    }
}
