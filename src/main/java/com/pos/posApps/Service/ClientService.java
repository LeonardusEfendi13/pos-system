package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.ClientDTO;
import com.pos.posApps.DTO.Dtos.CreateClientRequest;
import com.pos.posApps.DTO.Dtos.EditClientRequest;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Repository.ClientRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public ClientDTO getClientSettings(Long clientId) {
        try {
            Optional<ClientEntity> clientEntityOpt = clientRepository.findByClientIdAndDeletedAtIsNull(clientId);
            if (clientEntityOpt.isEmpty()) {
                return null;
            }

            ClientEntity clientEntity = clientEntityOpt.get();
            return new ClientDTO(
                    clientEntity.getClientId(),
                    clientEntity.getName(),
                    clientEntity.getAlamat(),
                    clientEntity.getKota(),
                    clientEntity.getNoTelp(),
                    clientEntity.getCatatan()
            );

        } catch (Exception e) {
            return null;
        }
    }

    public boolean updateClientField(Long clientId, String fieldKey, String fieldValue) {
        try {
            Optional<ClientEntity> clientOpt = clientRepository.findByClientIdAndDeletedAtIsNull(clientId);

            if (clientOpt.isEmpty()) return false;

            ClientEntity client = clientOpt.get();

            // Update manually based on key (since no reflection)
            switch (fieldKey) {
                case "NAMA" -> client.setName(fieldValue);
                case "ALAMAT" -> client.setAlamat(fieldValue);
                case "KOTA" -> client.setKota(fieldValue);
                case "NOMOR HP" -> client.setNoTelp(fieldValue);
                case "CATATAN" -> client.setCatatan(fieldValue);
                // You may want to block editing "Created At" and others
                default -> throw new IllegalArgumentException("Invalid field key: " + fieldKey);
            }
            clientRepository.save(client);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public boolean doCreateClient(CreateClientRequest req) {
        try {
            Long lastClientId = clientRepository.findFirstByOrderByClientIdDesc().map(ClientEntity::getClientId).orElse(0L);
            Long newClientId = Generator.generateId(lastClientId);
            ClientEntity clientEntity = new ClientEntity();
            clientEntity.setClientId(newClientId);
            clientEntity.setName(req.getName());
            clientEntity.setCreatedAt(getCurrentTimestamp());
            clientEntity.setUpdatedAt(getCurrentTimestamp());
            clientRepository.save(clientEntity);

            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public boolean doEditClient(EditClientRequest req) {
        try {
            Optional<ClientEntity> clientEntityOpt = clientRepository.findByClientIdAndDeletedAtIsNull(req.getClientId());
            if (clientEntityOpt.isEmpty()) {
                return false;
            }

            ClientEntity clientEntity = clientEntityOpt.get();

            clientEntity.setName(req.getName());
            clientEntity.setAlamat(req.getAlamat());
            clientEntity.setNoTelp(req.getNoTelp());
            clientEntity.setKota(req.getKota());
            clientEntity.setCatatan(req.getCatatan());
            clientRepository.save(clientEntity);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public boolean doDisableClient(Long clientId) {
        Optional<ClientEntity> clientEntityOpt = clientRepository.findByClientIdAndDeletedAtIsNull(clientId);
        if (clientEntityOpt.isEmpty()) {
            return false;
        }

        ClientEntity clientEntity = clientEntityOpt.get();

        clientEntity.setDeletedAt(getCurrentTimestamp());
        clientRepository.save(clientEntity);
        return true;
    }

}
