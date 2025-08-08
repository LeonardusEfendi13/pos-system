package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.CreatePreorderRequest;
import com.pos.posApps.DTO.Dtos.PreorderDTO;
import com.pos.posApps.DTO.Dtos.PreorderDetailDTO;
import com.pos.posApps.DTO.Dtos.ProductPricesDTO;
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
            return preorderRepository.findAllByClientEntity_ClientId(clientId);
        }else{
            return preorderRepository.findAllByClientEntity_ClientIdAndSupplierEntity_SupplierId(clientId, supplierId);
        }
    }

    @Transactional
    public boolean insertPreorder(CreatePreorderRequest req, ClientEntity clientData){
        try{
            String lastPreorderId = preorderRepository.findFirstByOrderByPreorderIdDesc().getPreorderId();
            String newPreorderId = Generator.generateId(lastPreorderId == null ? "POR0" : lastPreorderId);
            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierId(req.getSupplierId());
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
            String lastPreorderDetailsId = preorderDetailRepository.findFirstByOrderByPreorderDetailIdDesc().getPreorderDetailId();
            String newPreorderDetailsId = Generator.generateId(lastPreorderDetailsId == null ? "POL0" : lastPreorderDetailsId);

            for(PreorderDetailDTO preorderDetailData : req.getPreorderDetailData()){
                PreorderDetailEntity newData = new PreorderDetailEntity();

                ProductEntity productEntity = productRepository.findFirstByProductId(preorderDetailData.getProductId());
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
}
