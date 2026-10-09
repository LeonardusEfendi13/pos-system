package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.SidebarDTO;
import com.pos.posApps.Entity.AccountEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SidebarService {
    @Autowired
    private ClientService clientService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AccessResolver accessResolver;

    public SidebarDTO getSidebarData(Long clientId, String token) {
        String namaToko = clientService.getClientSettings(clientId).getName();
        AccountEntity account = authService.validateToken(token);
        EffectiveAccess access = accessResolver.resolve(account);
        return new SidebarDTO(
                namaToko,
                account.getAccountId(),
                account.getName(),
                access.role(),
                access.roleName(),
                access.menuKeys()
        );
    }
}
