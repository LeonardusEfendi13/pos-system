package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.KasirClientDTO;
import com.pos.posApps.DTO.Dtos.PreorderBootstrapDTO;
import com.pos.posApps.DTO.Dtos.PreorderDTO;
import com.pos.posApps.DTO.Dtos.SupplierLookupDTO;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
import com.pos.posApps.Service.PreorderService;
import com.pos.posApps.Service.SupplierService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/preorder")
@AllArgsConstructor
public class RestControllerPreorderBootstrap {
    private AuthService authService;
    private SupplierService supplierService;
    private ClientService clientService;
    private PreorderService preorderService;

    @GetMapping("/bootstrap")
    public ResponseEntity<PreorderBootstrapDTO> bootstrap(
            HttpSession session,
            @RequestParam(required = false) Long preorderId) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            var account = authService.validateToken(token);
            Long clientId = account.getClientEntity().getClientId();
            var suppliers = supplierService.getSupplierList(clientId).stream()
                    .map(supplier -> new SupplierLookupDTO(
                            supplier.getSupplierId(),
                            supplier.getSupplierName()))
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
            PreorderDTO preorder = null;
            if (preorderId != null) {
                preorder = preorderService.getPreorderDataById(clientId, preorderId);
            }
            return ResponseEntity.ok(
                    new PreorderBootstrapDTO(
                            suppliers,
                            client,
                            preorder,
                            account.getRole().name()));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }
}
