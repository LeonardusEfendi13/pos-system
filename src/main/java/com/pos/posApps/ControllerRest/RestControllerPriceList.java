package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.SuggestedPricesDTO;
import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.PriceListService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@RestController
@RequestMapping("api/price-list")
@AllArgsConstructor
public class RestControllerPriceList {

    private AuthService authService;
    private PriceListService priceListService;

    @GetMapping("/cek")
    public ResponseEntity<SuggestedPricesDTO> cekNewPrice(HttpSession session, @RequestParam("kode") String partNumber){
        String token;
        SuggestedPricesDTO suggestedPriceData = new SuggestedPricesDTO();
        try {
            token = (String) session.getAttribute(authSessionKey);
            AccountEntity accEntity = authService.validateToken(token);
            if (authService.hasAccessToModifyData(accEntity.getRole())) {
                if(accEntity.getRole() == Roles.SUPER_ADMIN){
                    suggestedPriceData = priceListService.getSuggestedPriceByPartNumber(partNumber);
                }
            }
            return ResponseEntity.ok(suggestedPriceData);
        } catch (Exception e) {
            return ResponseEntity.ok(suggestedPriceData);
        }

    }
}
