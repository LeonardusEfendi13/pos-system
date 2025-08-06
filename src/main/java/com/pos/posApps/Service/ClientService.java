package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.CreateClientDTO.CreateClientRequest;
import com.pos.posApps.DTO.Dtos.EditClientDTO.EditClientRequest;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Repository.ClientRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public boolean doCreateClient(CreateClientRequest req){
        String lastClientId = clientRepository.findFirstByOrderByClientIdDesc().getClientId();
        String newClientId;
        System.out.println("last client id : " + lastClientId);
        if(lastClientId == null){
            newClientId = Generator.generateId("CLN0");
        }else {
            newClientId = Generator.generateId(lastClientId);
        }

        ClientEntity clientEntity = new ClientEntity();
        clientEntity.setClientId(newClientId);
        clientEntity.setName(req.getName());
        clientEntity.setCreatedAt(getCurrentTimestamp());
        clientEntity.setUpdatedAt(getCurrentTimestamp());
        clientRepository.save(clientEntity);

        return true;
    }

    public boolean doEditClient(EditClientRequest req){
        ClientEntity clientEntity = clientRepository.findByClientId(req.getClientId());
        if(clientEntity == null){
            System.out.println("Client Not Found");
            return false;
        }

        clientEntity.setName(req.getName());
        clientRepository.save(clientEntity);
        return true;
    }

    public boolean doDisableClient(String clientId){
        ClientEntity clientEntity = clientRepository.findByClientId(clientId);
        if(clientEntity == null || clientEntity.getDeletedAt() != null){
            System.out.println("Client Not Found");
            return false;
        }

        clientEntity.setDeletedAt(getCurrentTimestamp());
        clientRepository.save(clientEntity);
        return true;
    }

}
