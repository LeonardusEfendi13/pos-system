package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.SidebarDTO;
import com.pos.posApps.DTO.Dtos.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SidebarService {
    @Autowired
    private ClientService clientService;

    @Autowired
    private AccountService accountService;
    public SidebarDTO getSidebarData(Long clientId, String token){
        String namaToko = clientService.getClientSettings(clientId).getName();
        UserDTO userData = accountService.getCurrentLoggedInUser(token);
        return new SidebarDTO(
                namaToko,
                userData.getUserId(),
                userData.getName(),
                userData.getRole()
        );
    }

}
