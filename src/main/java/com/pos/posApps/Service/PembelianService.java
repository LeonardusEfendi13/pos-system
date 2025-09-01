package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.pos.posApps.Util.Generator.generateNotaNumber;
import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class PembelianService {
    @Autowired
    TransactionDetailRepository transactionDetailRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    PurchasingRepository purchasingRepository;

    @Autowired
    SupplierRepository supplierRepository;

    public List<PembelianDTO> getPembelianData(Long clientId, LocalDateTime startDate, LocalDateTime endDate, Long supplierId) {
        List<PurchasingEntity> purchasingData;
        if(supplierId == null){
            purchasingData = purchasingRepository.findAllByClientEntity_ClientIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(clientId, startDate, endDate);
        }else{
            purchasingData = purchasingRepository.findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(clientId, supplierId, startDate, endDate);
        }
        return purchasingData.stream().map(purchasings -> new PembelianDTO(
                purchasings.getPurchasingId(),
                purchasings.getPurchasingNumber(),
                purchasings.getPoDate(),
                purchasings.isCash(),
                purchasings.getPoDueDate(),
                purchasings.getTotalPrice(),
                new SupplierDTO(
                        purchasings.getSupplierEntity().getSupplierId(),
                        purchasings.getSupplierEntity().getSupplierName()
                ),
                purchasings.isPaid(),
                purchasings.getPurchasingDetailEntities().stream()
                        .map(purchasingDetail -> new PembelianDetailDTO(
                                purchasingDetail.getPurchasingDetailId(),
                                purchasingDetail.getShortName(),
                                purchasingDetail.getFullName(),
                                purchasingDetail.getQty(),
                                purchasingDetail.getPrice(),
                                purchasingDetail.getDiscAmount(),
                                purchasingDetail.getTotalPrice(),
                                purchasingDetail.getMarkup1(),
                                purchasingDetail.getMarkup2(),
                                purchasingDetail.getMarkup3(),
                                purchasingDetail.getHargaJual1(),
                                purchasingDetail.getHargaJual2(),
                                purchasingDetail.getHargaJual3()
                        ))
                        .collect(Collectors.toList())  // collect the stream to a list
        )).collect(Collectors.toList());
    }

    public PembelianDTO getPembelianDataById(Long clientId, Long pembelianId) {
        PurchasingEntity purchasings = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, pembelianId);
        return new PembelianDTO(
                purchasings.getPurchasingId(),
                purchasings.getPurchasingNumber(),
                purchasings.getPoDate(),
                purchasings.isCash(),
                purchasings.getPoDueDate(),
                purchasings.getTotalPrice(),
                new SupplierDTO(
                        purchasings.getSupplierEntity().getSupplierId(),
                        purchasings.getSupplierEntity().getSupplierName()
                ),
                purchasings.isPaid(),
                purchasings.getPurchasingDetailEntities().stream()
                        .map(purchasingDetail -> new PembelianDetailDTO(
                                purchasingDetail.getPurchasingDetailId(),
                                purchasingDetail.getShortName(),
                                purchasingDetail.getFullName(),
                                purchasingDetail.getQty(),
                                purchasingDetail.getPrice(),
                                purchasingDetail.getDiscAmount(),
                                purchasingDetail.getTotalPrice(),
                                purchasingDetail.getMarkup1(),
                                purchasingDetail.getMarkup2(),
                                purchasingDetail.getMarkup3(),
                                purchasingDetail.getHargaJual1(),
                                purchasingDetail.getHargaJual2(),
                                purchasingDetail.getHargaJual3()
                        ))
                        .collect(Collectors.toList())
        );
    }

    @Transactional
    public boolean deletePenjualan(Long transactionId, Long clientId){
        //Restore stock from old transaction
        List<TransactionDetailEntity> oldTransactions = transactionDetailRepository.findAllByTransactionEntity_TransactionIdOrderByTransactionDetailIdDesc(transactionId);
        for(TransactionDetailEntity old : oldTransactions){
            ProductEntity product = productRepository.findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(old.getFullName(), old.getShortName(), clientId);
            if(product != null){
                Long restoredStock = product.getStock() + old.getQty();
                product.setStock(restoredStock);
                productRepository.save(product);
            }
        }

        TransactionEntity transactionEntity = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, transactionId);
        if(transactionEntity == null){
            System.out.println("Transaction not found");
            return false;
        }
        transactionEntity.setDeletedAt(getCurrentTimestamp());
        transactionRepository.save(transactionEntity);

        List<TransactionDetailEntity> transactionDetailEntities = transactionDetailRepository.findAllByTransactionEntity_TransactionIdOrderByTransactionDetailIdDesc(transactionId);

        for(TransactionDetailEntity data : transactionDetailEntities){
            data.setDeletedAt(getCurrentTimestamp());
            transactionDetailRepository.save(data);
        }

        return true;
    }

    @Transactional
    public String createTransaction(CreateTransactionRequest req, ClientEntity clientData){
        try{
            //Get Supplier Entity
            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getCustomerId(), clientData.getClientId());
            if (supplierEntity == null){
                return null;
            }
            //Get last Transaction id
            Long lastTransactionId = transactionRepository.findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByTransactionIdDesc(clientData.getClientId()).map(TransactionEntity::getTransactionId).orElse(0L);
            Long newTransactionId = Generator.generateId(lastTransactionId);
            String generatedNotaNumber = generateNotaNumber(newTransactionId);


            //insert the transaction data
            TransactionEntity transactionEntity = new TransactionEntity();
            transactionEntity.setClientEntity(clientData);
            transactionEntity.setTransactionNumber(generatedNotaNumber);
            transactionEntity.setTransactionId(newTransactionId);
//            transactionEntity.setCustomerEntity(customerEntity);
            transactionEntity.setTotalPrice(req.getTotalPrice());
            transactionEntity.setTotalDiscount(req.getTotalDisc());
            transactionEntity.setSubtotal(req.getSubTotal());
            transactionRepository.save(transactionEntity);

            System.out.println("Created transaction : " + newTransactionId);

            //Insert all the transaction details
            Long lastTransactionDetailId = transactionDetailRepository.findFirstByOrderByTransactionDetailIdDesc().map(TransactionDetailEntity::getTransactionDetailId).orElse(0L);
            Long newTransactionDetailId = Generator.generateId(lastTransactionDetailId);

            for(TransactionDetailDTO dtos : req.getTransactionDetailDtos()){
                TransactionDetailEntity transactionDetailEntity = new TransactionDetailEntity();
                transactionDetailEntity.setTransactionDetailId(newTransactionDetailId);
                transactionDetailEntity.setShortName(dtos.getCode());
                transactionDetailEntity.setFullName(dtos.getName());
                transactionDetailEntity.setQty(dtos.getQty());
                transactionDetailEntity.setPrice(dtos.getPrice());
                transactionDetailEntity.setDiscountAmount(dtos.getDiscAmount());
                transactionDetailEntity.setTotalPrice(dtos.getTotal());
                transactionDetailEntity.setTransactionEntity(transactionEntity);
                transactionDetailRepository.save(transactionDetailEntity);
                newTransactionDetailId = Generator.generateId(newTransactionDetailId);

                //Update product stock
                ProductEntity productEntity = productRepository.findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(dtos.getName(), dtos.getCode(), clientData.getClientId());
                Long newStock = productEntity.getStock() - dtos.getQty();
                productEntity.setStock(newStock);
                productRepository.save(productEntity);
            }
            return generatedNotaNumber;
        }catch (Exception e){
            System.out.println("Exception catched : " + e);
            return null;
        }
    }

}
