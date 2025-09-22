package com.pos.posApps.ControllerMVC;

import com.pos.posApps.DTO.Dtos.CreateClientRequest;
import com.pos.posApps.DTO.Dtos.EditClientRequest;
import com.pos.posApps.DTO.Dtos.EditUserRequest;
import com.pos.posApps.DTO.Dtos.RegisterFromDevRequest;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Service.AccountService;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.ClientService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.pos.posApps.Constants.Constant.authSessionKey;

//For Developer Only
@RestController
@RequestMapping("api/v1/god")
@AllArgsConstructor
public class DeveloperController {

    private AuthService authService;
    private ClientService clientService;
    private AccountService accountService;

    @PostMapping("/add-client")
    public String createClient(
            @Valid @RequestBody CreateClientRequest req,
            HttpSession session
    ) {
        String token = (String) session.getAttribute(authSessionKey);
        Long clientId = authService.validateToken(token).getClientEntity().getClientId();

        // Check if clientId is null or the clientId is not CLNO (Developer)
        if (isNotDeveloper(clientId)) {
            return "redirect:/login";
        }

        boolean isInserted = clientService.doCreateClient(req);
        if(isInserted){
            return "200";
        }
        return "500";
    }

    @PostMapping("/edit-client")
    public String editClient(
            @Valid EditClientRequest req,
            HttpSession session
    ){
        String token = (String) session.getAttribute(authSessionKey);
        Long clientId = authService.validateToken(token).getClientEntity().getClientId();
        // Check if clientId is null or the clientId is not CLNO (Developer)
        if (isNotDeveloper(clientId)) {
            return "redirect:/login";
        }
        boolean isUpdated = clientService.doEditClient(req);
        if(isUpdated){
            System.out.println("Client Edited");
            return "200";
        }
        System.out.println("Failed to edit client");
        return "500";
    }

    @PostMapping("/disable-client")
    public String disableClient(
            @Valid Long idClient,
            HttpSession session
    ){
        String token = (String) session.getAttribute(authSessionKey);
        Long clientId = authService.validateToken(token).getClientEntity().getClientId();

        // Check if clientId is null or the clientId is not CLNO (Developer)
        if (isNotDeveloper(clientId)) {
            return "redirect:/login";
        }

        boolean isDisabled = clientService.doDisableClient(idClient);

        if(isDisabled){
            return "200";
        }
        return "500";
    }

    @PostMapping("/add-user")
    public String createAccount(
            @RequestBody RegisterFromDevRequest req, HttpSession session)
    {
        System.out.println("req : " + req);
        String token = (String) session.getAttribute(authSessionKey);
        ClientEntity clientData = authService.validateToken(token).getClientEntity();

        // Check if clientId is null or the clientId is not CLNO (Developer)
        if (isNotDeveloper(clientData.getClientId())) {
            return "redirect:/login";
        }
        boolean isInserted = accountService.doCreateAccount(req.getRegisterRequest(), clientData);
        if(isInserted){
            return "201";
        }
        return "500";
    }

    @PostMapping("/edit-user")
    public String editUser(
            @Valid EditUserRequest req,
            HttpSession session
    ){
        String token = (String) session.getAttribute(authSessionKey);
        Long clientId = authService.validateToken(token).getClientEntity().getClientId();
        // Check if clientId is null or the clientId is not CLNO (Developer)
        if (isNotDeveloper(clientId)) {
            return "redirect:/login";
        }
        boolean isUpdated = accountService.doUpdateAccount(req);
        if(isUpdated){
            return "201";
        }
        return "500";
    }

    @PostMapping("/disable-user")
    public String disableUser(
            @Valid Long idUser,
            HttpSession session
    ){
        String token = (String) session.getAttribute(authSessionKey);
        Long clientId = authService.validateToken(token).getClientEntity().getClientId();

        // Check if clientId is null or the clientId is not CLNO (Developer)
        if (isNotDeveloper(clientId)) {
            return "redirect:/login";
        }

        boolean isDisabled = accountService.doDisableAccount(idUser);

        if(isDisabled){
            return "200";
        }
        return "500";
    }

    private boolean isNotDeveloper(Long clientId){
        return (clientId == null || clientId != 0L);
    }
}
