package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreatePreorderRequest;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.KasirService;
import com.pos.posApps.Service.PreorderService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/preorder")
@AllArgsConstructor
public class RestControllerPreorder {
    private AuthService authService;
    private KasirService kasirService;
    private PreorderService preorderService;

    @PostMapping("/add")
    public ResponseEntity<String> addTransaction(@RequestBody CreatePreorderRequest req, HttpSession session){
        ClientEntity clientData;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientData = authService.validateToken(token).getClientEntity();
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }
        ResponseInBoolean response = preorderService.createTransaction(req, clientData);
        if(response.isStatus()){
            return ResponseEntity.ok(response.getMessage());
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }

    @PostMapping("/edit/{preorderId}")
    public ResponseEntity<String> editTransaction(@PathVariable("preorderId") Long preorderId, @RequestBody CreatePreorderRequest req, HttpSession session){
        Long clientId;
        try {
            String token = (String) session.getAttribute(authSessionKey);
            clientId = authService.validateToken(token).getClientEntity().getClientId();
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).body("Unauthorized access");
        }

        ResponseInBoolean response = preorderService.editTransaction(preorderId, req, clientId);
        if(response.isStatus()){
            return ResponseEntity.ok(response.getMessage());
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response.getMessage());
    }
}
