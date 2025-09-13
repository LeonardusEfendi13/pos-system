package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.PreorderDetailRepository;
import com.pos.posApps.Repository.PreorderRepository;
import com.pos.posApps.Repository.ProductRepository;
import com.pos.posApps.Repository.SupplierRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class PreorderService {
    @Autowired
    PreorderRepository preorderRepository;

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    PreorderDetailRepository preorderDetailRepository;

    public List<PreorderEntity> getPreorderData(Long clientId, Long supplierId){
        if(supplierId == null){
            return preorderRepository.findAllByClientEntity_ClientIdOrderByPreorderIdDesc(clientId);
        }else{
            return preorderRepository.findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdOrderByPreorderIdDesc(clientId, supplierId);
        }
    }

    @Transactional
    public boolean insertPreorder(CreatePreorderRequest req, ClientEntity clientData){
        try{
            Long lastPreorderId = preorderRepository.findFirstByOrderByPreorderIdDesc().map(PreorderEntity::getPreorderId).orElse(0L);
            Long newPreorderId = Generator.generateId(lastPreorderId);
            Optional<SupplierEntity> supplierEntityOpt = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientData.getClientId());
            if(supplierEntityOpt.isEmpty()){
                System.out.println("Can't find supplier with id : " + req.getSupplierId());
                return false;
            }

            SupplierEntity supplierEntity = supplierEntityOpt.get();

            //Insert Preorder
            PreorderEntity newPreorder = new PreorderEntity();
            newPreorder.setPreorderId(newPreorderId);
            newPreorder.setSupplierEntity(supplierEntity);
            newPreorder.setClientEntity(clientData);
            preorderRepository.save(newPreorder);

            //Insert Preorder Details
            Long lastPreorderDetailsId = preorderDetailRepository.findFirstByOrderByPreorderDetailIdDesc().map(PreorderDetailEntity::getPreorderDetailId).orElse(0L);
            Long newPreorderDetailsId = Generator.generateId(lastPreorderDetailsId);

            for(PreorderDetailDTO preorderDetailData : req.getPreorderDetailData()){
                PreorderDetailEntity newData = new PreorderDetailEntity();

                ProductEntity productEntity = productRepository.findFirstByProductIdAndDeletedAtIsNull(preorderDetailData.getProductId());
                newData.setPreorderDetailId(newPreorderDetailsId);
                newData.setQuantity(preorderDetailData.getQuantity());
                newData.setPreorderEntity(newPreorder);
                //todo
//                newData.setProductEntity(productEntity);

                //Generate for next loop
                newPreorderDetailsId = Generator.generateId(newPreorderDetailsId);
                preorderDetailRepository.save(newData);
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Transactional
    public boolean editPreorder(EditPreorderRequest req, ClientEntity clientData){
        try{
            Optional<SupplierEntity> supplierEntityOpt = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientData.getClientId());
            if(supplierEntityOpt.isEmpty()){
                System.out.println("Can't find supplier with id : " + req.getSupplierId());
                return false;
            }
            SupplierEntity supplierEntity = supplierEntityOpt.get();

            //Insert Preorder
            PreorderEntity newPreorder = preorderRepository.findFirstByPreorderIdAndDeletedAtIsNull(req.getPreorderId());
            if(newPreorder == null){
                System.out.println("Preorder data not found");
                return false;
            }
            newPreorder.setSupplierEntity(supplierEntity);
            newPreorder.setClientEntity(clientData);
            preorderRepository.save(newPreorder);

            for(PreorderDetailDTO preorderDetailData : req.getPreorderDetailData()){
                PreorderDetailEntity newData = preorderDetailRepository.findFirstByPreorderDetailId(preorderDetailData.getPreorderDetailId());

                ProductEntity productEntity = productRepository.findFirstByProductIdAndDeletedAtIsNull(preorderDetailData.getProductId());
                newData.setQuantity(preorderDetailData.getQuantity());
                newData.setPreorderEntity(newPreorder);
                //todo
//                newData.setProductEntity(productEntity);
                preorderDetailRepository.save(newData);
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Transactional
    public boolean deleteProducts(Long preorderId){
        PreorderEntity preorderEntity = preorderRepository.findFirstByPreorderIdAndDeletedAtIsNull(preorderId);
        if(preorderEntity == null){
            System.out.println("Preorder not found");
            return false;
        }
        preorderEntity.setDeletedAt(getCurrentTimestamp());
        preorderRepository.save(preorderEntity);

        List<PreorderDetailEntity> preorderDetailEntities = preorderDetailRepository.findAllByPreorderEntity_PreorderIdOrderByPreorderDetailIdDesc(preorderId);

        for(PreorderDetailEntity data : preorderDetailEntities){
            data.setDeletedAt(getCurrentTimestamp());
            preorderDetailRepository.save(data);
        }
        return true;
    }
}
