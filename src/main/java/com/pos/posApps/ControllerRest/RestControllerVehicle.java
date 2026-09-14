package com.pos.posApps.ControllerRest;

import com.pos.posApps.DTO.Dtos.CreateVehicleRequest;
import com.pos.posApps.DTO.Dtos.EditVehicleRequest;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.VehicleDTO;
import com.pos.posApps.DTO.Dtos.VehicleListResponseDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.VehicleEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.VehicleService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("api/vehicle")
@AllArgsConstructor
public class RestControllerVehicle {
    private AuthService authService;
    private VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<VehicleListResponseDTO> list(HttpSession session) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new VehicleListResponseDTO(
                mapList(vehicleService.getVehicleList("YAMAHA")),
                mapList(vehicleService.getVehicleList("HONDA")),
                account.getRole().name(),
                account.getName()
        ));
    }

    @PostMapping
    public ResponseEntity<ResponseInBoolean> add(HttpSession session, @RequestBody CreateVehicleRequest req) {
        return mutate(session, account -> vehicleService.insertVehicle(
                req.getModel(),
                req.getBrand(),
                req.getKnownPartNumber()
        ));
    }

    @PostMapping("/edit")
    public ResponseEntity<ResponseInBoolean> edit(HttpSession session, @RequestBody EditVehicleRequest req) {
        return mutate(session, account -> vehicleService.editVehicle(
                req.getVehicleId(),
                req.getModel(),
                req.getBrand(),
                req.getKnownPartNumber()
        ));
    }

    @PostMapping("/delete/{vehicleId}")
    public ResponseEntity<ResponseInBoolean> delete(HttpSession session, @PathVariable Long vehicleId) {
        return mutate(session, account -> {
            boolean deleted = vehicleService.deleteVehicle(vehicleId);
            if (deleted) {
                return new ResponseInBoolean(true, "Data Deleted");
            }
            return new ResponseInBoolean(false, "Failed to delete data");
        });
    }

    private AccountEntity requireAccount(HttpSession session) {
        try {
            String token = (String) session.getAttribute(authSessionKey);
            return authService.validateToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<ResponseInBoolean> mutate(
            HttpSession session,
            java.util.function.Function<AccountEntity, ResponseInBoolean> action
    ) {
        AccountEntity account = requireAccount(session);
        if (account == null) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Harap login ulang"));
        }
        if (!authService.hasAccessToModifyData(account.getRole())) {
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ResponseInBoolean(false, "Anda tidak memiliki akses untuk ini!"));
        }

        ResponseInBoolean result = action.apply(account);
        if (result.isStatus()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(result);
    }

    private List<VehicleDTO> mapList(List<VehicleEntity> vehicles) {
        if (vehicles == null) {
            return List.of();
        }

        return vehicles.stream().map(this::toDto).toList();
    }

    private VehicleDTO toDto(VehicleEntity vehicle) {
        return new VehicleDTO(
                vehicle.getId(),
                vehicle.getModel(),
                vehicle.getBrand(),
                vehicle.getKnownPartNumber()
        );
    }
}
