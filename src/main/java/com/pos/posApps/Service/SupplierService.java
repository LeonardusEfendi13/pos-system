package com.pos.posApps.Service;

import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Repository.SupplierRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class SupplierService {
    @Autowired
    SupplierRepository supplierRepository;

    public List<SupplierEntity> getSupplierList(String clientId){
        return supplierRepository.findAllByClientEntity_ClientId(clientId);
    }

    @Transactional
    public Boolean insertSupplier(String supplierName, ClientEntity clientData){
        try{
            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierNameAndClientEntity_ClientId(supplierName, clientData.getClientId());
            if(supplierEntity == null){
                System.out.println("Supplier alr exists");
                return false;
            }

            String lastSupplierId = supplierRepository.findFirstByOrderBySupplierIdDesc().getSupplierId();
            String newSupplierId = Generator.generateId(lastSupplierId == null ? "SPP0" : lastSupplierId);

            SupplierEntity newSupplierEntity = new SupplierEntity();
            newSupplierEntity.setSupplierId(newSupplierId);
            newSupplierEntity.setSupplierName(supplierName);
            newSupplierEntity.setClientEntity(clientData);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Transactional
    public Boolean editSupplier(String supplierId, String supplierName, ClientEntity clientData){
        try{
            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierIdAndClientEntity_ClientId(supplierId, clientData.getClientId());
            if(supplierEntity == null || supplierEntity.getDeletedAt() != null){
                return false;
            }

            supplierEntity.setSupplierName(supplierName);
            supplierRepository.save(supplierEntity);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Transactional
    public Boolean disableSupplier(String supplierId, ClientEntity clientData){
        try{
            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierIdAndClientEntity_ClientId(supplierId, clientData.getClientId());
            if(supplierEntity == null || supplierEntity.getDeletedAt() != null){
                return false;
            }

            supplierEntity.setDeletedAt(getCurrentTimestamp());
            supplierRepository.save(supplierEntity);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
