package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.PreorderDetailRepository;
import com.pos.posApps.Repository.PreorderRepository;
import com.pos.posApps.Repository.ProductRepository;
import com.pos.posApps.Repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Comparator;


import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class PreorderService {
    @Autowired
    PreorderRepository preorderRepository;

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    PreorderDetailRepository preorderDetailRepository;

    public Page<PreorderEntity> getPreorderData(Long clientId, Long supplierId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        if (supplierId == null) {
            return preorderRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByPreorderIdDesc(clientId, startDate, endDate, pageable);
        } else {
            return preorderRepository.findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByPreorderIdDesc(clientId, supplierId, startDate, endDate, pageable);
        }
    }

    public Page<PreorderEntity> searchPreorderData(Long clientId, Long supplierId, LocalDateTime startDate, LocalDateTime endDate, String search, Pageable pageable) {
        String trimmedSearch = (search != null) ? search.trim() : "";

        if (trimmedSearch.isEmpty()) {
            return getPreorderData(clientId, supplierId, startDate, endDate, pageable);
        }

        return preorderRepository.searchPreorders(
                clientId,
                supplierId,
                startDate,
                endDate,
                trimmedSearch,
                pageable
        );
    }

    public PreorderDTO getPreorderDataById(Long clientId, Long preorderId) {
        Optional<PreorderEntity> preorderOpt = preorderRepository.findFirstByClientEntity_ClientIdAndPreorderIdAndPreorderDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, preorderId);
        if (preorderOpt.isEmpty()) {
            return null;
        }
        PreorderEntity preorders = preorderOpt.get();
        return new PreorderDTO(
                preorders.getPreorderId(),
                new SupplierDTO(
                        preorders.getSupplierEntity().getSupplierId(),
                        preorders.getSupplierEntity().getSupplierName()
                ),
                preorders.getSubtotal(),
                preorders.getTotalPrice(),
                preorders.getTotalDiscount(),
                preorders.getCreatedAt(),
                preorders.getPreorderDetailEntities().stream()
                        .map(preorderDetail -> new PreorderDetailDTO(
                                preorderDetail.getPreorderDetailId(),
                                preorderDetail.getShortName(),
                                preorderDetail.getFullName(),
                                preorderDetail.getQuantity(),
                                preorderDetail.getPrice(),
                                preorderDetail.getDiscountAmount(),
                                preorderDetail.getTotalPrice()
                        ))
                        .collect(Collectors.toList())
        );
    }

    @Transactional
    public ResponseInBoolean createTransaction(CreatePreorderRequest req, ClientEntity clientData) {
        try {
            //Get Supplier Entity
            Optional<SupplierEntity> supplierEntityOpt = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientData.getClientId());
            if (supplierEntityOpt.isEmpty()) {
                return new ResponseInBoolean(true, "Supplier tidak ada");
            }
            SupplierEntity supplierEntity = supplierEntityOpt.get();

            if(req.getPreorderDetailDTOS() == null){
                return new ResponseInBoolean(true, "Data tidak diterima");
            }

            //Get last Transaction id
//            Long lastPreorderId = preorderRepository.findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByPreorderIdDesc(clientData.getClientId()).map(PreorderEntity::getPreorderId).orElse(0L);
//            Long newPreorderId = Generator.generateId(lastPreorderId);

            PreorderEntity preorderEntity = new PreorderEntity();
//            preorderEntity.setPreorderId(newPreorderId);
            preorderEntity.setSupplierEntity(supplierEntity);
            preorderEntity.setClientEntity(clientData);
            preorderEntity.setSubtotal(req.getSubtotal());
            preorderEntity.setTotalPrice(req.getTotalPrice());
            preorderEntity.setTotalDiscount(req.getTotalDisc());
            preorderRepository.save(preorderEntity);

            // ✅ Sort preorder details alphabetically by name (A–Z)
            if (req.getPreorderDetailDTOS() != null && !req.getPreorderDetailDTOS().isEmpty()) {
                req.getPreorderDetailDTOS().sort(Comparator.comparing(PreorderDetailDTO::getName, String.CASE_INSENSITIVE_ORDER));
            }

            //Insert all the transaction details
//            Long lastPreorderDetailId = preorderDetailRepository.findFirstByDeletedAtIsNullOrderByPreorderDetailIdDesc().map(PreorderDetailEntity::getPreorderDetailId).orElse(0L);
//            Long newPreorderDetailId = Generator.generateId(lastPreorderDetailId);

            for (PreorderDetailDTO dtos : req.getPreorderDetailDTOS()) {

                PreorderDetailEntity preorderDetailEntity = new PreorderDetailEntity();
//                preorderDetailEntity.setPreorderDetailId(newPreorderDetailId);
                preorderDetailEntity.setShortName(dtos.getCode());
                preorderDetailEntity.setFullName(dtos.getName());
                preorderDetailEntity.setQuantity(dtos.getQty());
                preorderDetailEntity.setPrice(dtos.getPrice());
                preorderDetailEntity.setDiscountAmount(dtos.getDiscAmount());
                preorderDetailEntity.setTotalPrice(dtos.getTotal());
                preorderDetailEntity.setPreorderEntity(preorderEntity);
                preorderDetailRepository.save(preorderDetailEntity);
//                newPreorderDetailId = Generator.generateId(newPreorderDetailId);
            }
            return new ResponseInBoolean(true, "Preorder berhasil dibuat");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage());
        }
    }

    @Transactional
    public ResponseInBoolean editTransaction(Long preorderId, CreatePreorderRequest req, Long clientId){
        try{
            //Get Supplier Entity
            Optional<SupplierEntity> supplierEntityOpt = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientId);
            if (supplierEntityOpt.isEmpty()) {
                return new ResponseInBoolean(true, "Supplier tidak ada");
            }
            SupplierEntity supplierEntity = supplierEntityOpt.get();

            //Check if transaction exist
            Optional<PreorderEntity> preorderEntityOpt = preorderRepository.findFirstByClientEntity_ClientIdAndPreorderIdAndPreorderDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, preorderId);

            if(preorderEntityOpt.isEmpty()){
                return new ResponseInBoolean(false, "Data transaksi tidak ditemukan");
            }
            PreorderEntity preorderEntity = preorderEntityOpt.get();

            //insert the transaction data
            preorderEntity.setSupplierEntity(supplierEntity);
            preorderEntity.setTotalPrice(req.getTotalPrice());
            preorderEntity.setTotalDiscount(req.getTotalDisc());
            preorderEntity.setSubtotal(req.getSubtotal());
            preorderRepository.save(preorderEntity);

            //Delete all product prices related to product id
            preorderDetailRepository.deleteAllByPreorderEntity_PreorderId(preorderId);

            // ✅ Sort preorder details alphabetically by name (A–Z)
            if (req.getPreorderDetailDTOS() != null && !req.getPreorderDetailDTOS().isEmpty()) {
                req.getPreorderDetailDTOS().sort(Comparator.comparing(PreorderDetailDTO::getName, String.CASE_INSENSITIVE_ORDER));
            }

            //Insert all the transaction details
//            Long lastPreorderDetailId = preorderDetailRepository.findFirstByDeletedAtIsNullOrderByPreorderDetailIdDesc().map(PreorderDetailEntity::getPreorderDetailId).orElse(0L);
//            Long newPreorderDetailId = Generator.generateId(lastPreorderDetailId);

            for(PreorderDetailDTO dtos : req.getPreorderDetailDTOS()){
                if(dtos != null) {
                    PreorderDetailEntity preorderDetailEntity = new PreorderDetailEntity();
//                    preorderDetailEntity.setPreorderDetailId(newPreorderDetailId);
                    preorderDetailEntity.setShortName(dtos.getCode());
                    preorderDetailEntity.setFullName(dtos.getName());
                    preorderDetailEntity.setQuantity(dtos.getQty());
                    preorderDetailEntity.setPrice(dtos.getPrice());
                    preorderDetailEntity.setDiscountAmount(dtos.getDiscAmount());
                    preorderDetailEntity.setTotalPrice(dtos.getTotal());
                    preorderDetailEntity.setPreorderEntity(preorderEntity);
                    preorderDetailRepository.save(preorderDetailEntity);
//                    newPreorderDetailId = Generator.generateId(newPreorderDetailId);
                }
            }
            return new ResponseInBoolean(true, "Data berhasil disimpan");
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage());
        }
    }

    @Transactional
    public boolean deletePreorder(Long purchasingId, ClientEntity clientData) {
        Optional<PreorderEntity> preorderEntityOpt = preorderRepository.findFirstByClientEntity_ClientIdAndPreorderIdAndDeletedAtIsNull(clientData.getClientId(), purchasingId);
        if (preorderEntityOpt.isEmpty()) {
            return false;
        }
        PreorderEntity preorderEntity = preorderEntityOpt.get();

        preorderEntity.setDeletedAt(getCurrentTimestamp());
        preorderRepository.save(preorderEntity);

        return true;
    }
}
