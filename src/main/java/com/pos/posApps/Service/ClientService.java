package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.CreateClientRequest;
import com.pos.posApps.DTO.Dtos.EditClientRequest;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Repository.ClientRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public boolean doCreateClient(CreateClientRequest req){
        try{
            String lastClientId = clientRepository.findFirstByOrderByClientIdDesc().map(ClientEntity::getClientId).orElse("CLN0");
            String newClientId = Generator.generateId(lastClientId);
            ClientEntity clientEntity = new ClientEntity();
            clientEntity.setClientId(newClientId);
            clientEntity.setName(req.getName());
            clientEntity.setCreatedAt(getCurrentTimestamp());
            clientEntity.setUpdatedAt(getCurrentTimestamp());
            clientRepository.save(clientEntity);

            return true;
        }catch (Exception e){
            return false;
        }

    }

    public boolean doEditClient(EditClientRequest req){
        ClientEntity clientEntity = clientRepository.findByClientIdAndDeletedAtIsNull(req.getClientId());
        if(clientEntity == null){
            System.out.println("Client Not Found");
            return false;
        }

        clientEntity.setName(req.getName());
        clientRepository.save(clientEntity);
        return true;
    }

    public boolean doDisableClient(String clientId){
        ClientEntity clientEntity = clientRepository.findByClientIdAndDeletedAtIsNull(clientId);
        if(clientEntity == null){
            System.out.println("Client Not Found");
            return false;
        }

        clientEntity.setDeletedAt(getCurrentTimestamp());
        clientRepository.save(clientEntity);
        return true;
    }

}
