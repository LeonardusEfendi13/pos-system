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

    public List<SupplierEntity> getSupplierList(Long clientId){
        return supplierRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderBySupplierIdDesc(clientId);
    }

    @Transactional
    public Boolean insertSupplier(String supplierName, ClientEntity clientData){
        try{
            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierNameIgnoreCaseAndClientEntity_ClientIdAndDeletedAtIsNull(supplierName, clientData.getClientId());
            if(supplierEntity != null){
                return false;
            }

            SupplierEntity lastSupplierData = supplierRepository.findFirstByOrderBySupplierIdDesc();
            Long lastSupplierId = lastSupplierData == null ? 0L : lastSupplierData.getSupplierId();
            Long newSupplierId = Generator.generateId(lastSupplierId);
            SupplierEntity newSupplierEntity = new SupplierEntity();
            newSupplierEntity.setSupplierId(newSupplierId);
            newSupplierEntity.setSupplierName(supplierName);
            newSupplierEntity.setClientEntity(clientData);
            supplierRepository.save(newSupplierEntity);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Transactional
    public Boolean editSupplier(Long supplierId, String supplierName, ClientEntity clientData){
        try{
            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierIdAndClientEntity_ClientIdAndDeletedAtIsNull(supplierId, clientData.getClientId());
            if(supplierEntity == null){
                return false;
            }

            // Check if another supplier with the same name exists (excluding the current one)
            boolean nameExists = supplierRepository.existsBySupplierNameIgnoreCaseAndClientEntity_ClientIdAndDeletedAtIsNullAndSupplierIdNot(
                    supplierName, clientData.getClientId(), supplierId
            );

            if (nameExists) {
                return false; // Duplicate name exists
            }

            supplierEntity.setSupplierName(supplierName);
            supplierRepository.save(supplierEntity);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Transactional
    public Boolean disableSupplier(Long supplierId, ClientEntity clientData){
        try{
            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierIdAndClientEntity_ClientIdAndDeletedAtIsNull(supplierId, clientData.getClientId());
            if(supplierEntity == null){
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
