package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.ClientDTO;
import com.pos.posApps.DTO.Dtos.CreateClientRequest;
import com.pos.posApps.DTO.Dtos.EditClientRequest;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.UpdateClientSettingsRequest;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
                    clientEntity.getCatatan(),
                    clientEntity.getKingDiscYmh(),
                    clientEntity.getKingDiscHnd()
            );

        } catch (Exception e) {
            return null;
        }
    }

    public ResponseInBoolean updateClientField(Long clientId, String fieldKey, String fieldValue) {
        try {
            Optional<ClientEntity> clientOpt = clientRepository.findByClientIdAndDeletedAtIsNull(clientId);

            if (clientOpt.isEmpty()) return new ResponseInBoolean(false, "Data client tidak ditemukan");

            ClientEntity client = clientOpt.get();
            System.out.println("fields : " + fieldKey + " | " + fieldValue);

            // Update manually based on key (since no reflection)
            switch (fieldKey) {
                case "NAMA" -> client.setName(fieldValue);
                case "ALAMAT" -> client.setAlamat(fieldValue);
                case "KOTA" -> client.setKota(fieldValue);
                case "NOMOR HP" -> client.setNoTelp(fieldValue);
                case "CATATAN" -> client.setCatatan(fieldValue);
                case "KING DISC YAMAHA" -> {
                    String cleanValue = fieldValue.replaceAll("[^\\d.]", "");
                    BigDecimal kingDisc = new BigDecimal(cleanValue);
                    if(kingDisc.compareTo(BigDecimal.valueOf(95))>0){
                        return new ResponseInBoolean(false, "King Discount tidak boleh lebih besar dari 95%");
                    }
                    client.setKingDiscYmh(kingDisc);
                }
                case "KING DISC HONDA" -> {
                    String cleanValue = fieldValue.replaceAll("[^\\d.]", "");
                    BigDecimal kingDisc = new BigDecimal(cleanValue);
                    if(kingDisc.compareTo(BigDecimal.valueOf(95))>0){
                        return new ResponseInBoolean(false, "King Discount tidak boleh lebih besar dari 95%");
                    }
                    client.setKingDiscHnd(kingDisc);
                }
                default -> throw new IllegalArgumentException("Invalid field key: " + fieldKey);
            }
            clientRepository.save(client);
            return new ResponseInBoolean(true, "Berhasil update setting");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseInBoolean(false, "Gagal Update data, ada yg salah nih : " + e.getMessage());
        }
    }

    public ResponseInBoolean updateClientSettings(Long clientId, UpdateClientSettingsRequest req) {
        try {
            Optional<ClientEntity> clientOpt = clientRepository.findByClientIdAndDeletedAtIsNull(clientId);
            if (clientOpt.isEmpty()) {
                return new ResponseInBoolean(false, "Data client tidak ditemukan");
            }

            // Validate name
            String name = req.getName() == null ? "" : req.getName().trim().toUpperCase();
            if (name.isEmpty()) {
                return new ResponseInBoolean(false, "Nama toko tidak boleh kosong");
            }

            // Validate kingDiscYmh
            if (req.getKingDiscYmh() != null && req.getKingDiscYmh().compareTo(BigDecimal.valueOf(95)) > 0) {
                return new ResponseInBoolean(false, "King Discount tidak boleh lebih besar dari 95%");
            }

            // Validate kingDiscHnd
            if (req.getKingDiscHnd() != null && req.getKingDiscHnd().compareTo(BigDecimal.valueOf(95)) > 0) {
                return new ResponseInBoolean(false, "King Discount tidak boleh lebih besar dari 95%");
            }

            ClientEntity client = clientOpt.get();
            client.setName(name);
            client.setAlamat(req.getAlamat() == null ? "" : req.getAlamat().trim().toUpperCase());
            client.setKota(req.getKota() == null ? "" : req.getKota().trim().toUpperCase());
            client.setNoTelp(req.getNoTelp() == null ? "" : req.getNoTelp().replaceAll("[^\\d]", ""));
            client.setCatatan(req.getCatatan() == null ? "" : req.getCatatan().trim().toUpperCase());
            client.setKingDiscYmh(req.getKingDiscYmh());
            client.setKingDiscHnd(req.getKingDiscHnd());
            clientRepository.save(client);

            return new ResponseInBoolean(true, "Berhasil update setting");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseInBoolean(false, "Gagal update setting: " + e.getMessage());
        }
    }

    @Transactional
    public boolean doCreateClient(CreateClientRequest req) {
        try {
            ClientEntity clientEntity = new ClientEntity();
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
