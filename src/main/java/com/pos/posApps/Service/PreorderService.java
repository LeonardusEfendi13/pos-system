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

    public List<PreorderEntity> getPreorderData(String clientId, String supplierId){
        if(supplierId == null || supplierId.isBlank()){
            return preorderRepository.findAllByClientEntity_ClientIdOrderByCreatedAtDesc(clientId);
        }else{
            return preorderRepository.findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdOrderByCreatedAtDesc(clientId, supplierId);
        }
    }

    @Transactional
    public boolean insertPreorder(CreatePreorderRequest req, ClientEntity clientData){
        try{
            String lastPreorderId = preorderRepository.findFirstByOrderByCreatedAtDesc().map(PreorderEntity::getPreorderId).orElse("POR0");
            String newPreorderId = Generator.generateId(lastPreorderId);
            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNull(req.getSupplierId());
            if(supplierEntity == null){
                System.out.println("Can't find supplier with id : " + req.getSupplierId());
                return false;
            }

            //Insert Preorder
            PreorderEntity newPreorder = new PreorderEntity();
            newPreorder.setPreorderId(newPreorderId);
            newPreorder.setSupplierEntity(supplierEntity);
            newPreorder.setClientEntity(clientData);
            preorderRepository.save(newPreorder);

            //Insert Preorder Details
            String lastPreorderDetailsId = preorderDetailRepository.findFirstByOrderByCreatedAtDesc().map(PreorderDetailEntity::getPreorderDetailId).orElse("POL0");
            String newPreorderDetailsId = Generator.generateId(lastPreorderDetailsId);

            for(PreorderDetailDTO preorderDetailData : req.getPreorderDetailData()){
                PreorderDetailEntity newData = new PreorderDetailEntity();

                ProductEntity productEntity = productRepository.findFirstByProductIdAndDeletedAtIsNull(preorderDetailData.getProductId());
                newData.setPreorderDetailId(newPreorderDetailsId);
                newData.setQuantity(preorderDetailData.getQuantity());
                newData.setPreorderEntity(newPreorder);
                newData.setProductEntity(productEntity);

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
            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNull(req.getSupplierId());
            if(supplierEntity == null){
                System.out.println("Can't find supplier with id : " + req.getSupplierId());
                return false;
            }

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
                newData.setProductEntity(productEntity);
                preorderDetailRepository.save(newData);
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Transactional
    public boolean deleteProducts(String preorderId){
        PreorderEntity preorderEntity = preorderRepository.findFirstByPreorderIdAndDeletedAtIsNull(preorderId);
        if(preorderEntity == null){
            System.out.println("Preorder not found");
            return false;
        }
        preorderEntity.setDeletedAt(getCurrentTimestamp());
        preorderRepository.save(preorderEntity);

        List<PreorderDetailEntity> preorderDetailEntities = preorderDetailRepository.findAllByPreorderEntity_PreorderIdOrderByCreatedAtDesc(preorderId);

        for(PreorderDetailEntity data : preorderDetailEntities){
            data.setDeletedAt(getCurrentTimestamp());
            preorderDetailRepository.save(data);
        }
        return true;
    }
}
