package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.ConvertToPembelianDTO;
import com.pos.posApps.DTO.Dtos.KasirClientDTO;
import com.pos.posApps.DTO.Dtos.PembelianBootstrapDTO;
import com.pos.posApps.DTO.Dtos.PembelianDTO;
import com.pos.posApps.DTO.Dtos.PreorderDTO;
import com.pos.posApps.DTO.Dtos.SupplierLookupDTO;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
import com.pos.posApps.Service.PembelianService;
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
@RequestMapping("api/pembelian")
@AllArgsConstructor
public class RestControllerPembelianBootstrap {
    private AuthService authService;
    private SupplierService supplierService;
    private ClientService clientService;
    private PembelianService pembelianService;
    private PreorderService preorderService;

    @GetMapping("/bootstrap")
    public ResponseEntity<PembelianBootstrapDTO> bootstrap(
            HttpSession session,
            @RequestParam(required = false) Long pembelianId,
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

            PembelianDTO pembelian = null;
            ConvertToPembelianDTO convert = null;
            if (pembelianId != null) {
                pembelian = pembelianService.getPembelianDataById(clientId, pembelianId);
            } else if (preorderId != null) {
                PreorderDTO preorder = preorderService.getPreorderDataById(clientId, preorderId);
                if (preorder != null) {
                    convert = preorderService.prepareDataForKasirPembelian(
                            clientId,
                            preorder,
                            preorderId);
                }
            }

            return ResponseEntity.ok(
                    new PembelianBootstrapDTO(
                            suppliers,
                            client,
                            pembelian,
                            convert,
                            account.getRole().name()));
        } catch (Exception e) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }
    }
}
