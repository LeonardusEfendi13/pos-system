package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import com.pos.posApps.Util.Generator;
import org.hibernate.sql.Insert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.pos.posApps.Util.Generator.generateId;
import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class KasirService {
    @Autowired
    ProductRepository productRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    TransactionDetailRepository transactionDetailRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Transactional
    public boolean createTransaction(CreateTransactionRequest req, ClientEntity clientData){
        try{
            //Get Supplier Entity
            CustomerEntity customerEntity = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getCustomerId(), clientData.getClientId());
            if (customerEntity == null){
                return false;
            }
            //Get last Transaction id
            Long lastTransactionId = transactionRepository.findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByTransactionIdDesc(clientData.getClientId()).map(TransactionEntity::getTransactionId).orElse(0L);
            Long newTransactionId = Generator.generateId(lastTransactionId);

            //insert the transaction data
            TransactionEntity transactionEntity = new TransactionEntity();
            transactionEntity.setClientEntity(clientData);
            transactionEntity.setTransactionId(newTransactionId);
            transactionEntity.setCustomerEntity(customerEntity);
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
            return true;
        }catch (Exception e){
            System.out.println("Exception catched : " + e);
            return false;
        }
    }

    @Transactional
    public boolean editTransaction(Long transactionId, CreateTransactionRequest req, Long clientId){
        try{
            //Get Supplier Entity
            CustomerEntity customerEntity = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getCustomerId(), clientId);
            if (customerEntity == null){
                System.out.println("customer ga nemu");
                return false;
            }
            //Check if transaction exist
            TransactionEntity transactionEntity = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, transactionId);
            if(transactionEntity == null){
                System.out.println("transaction ga nemu");
                return false;
            }

            //insert the transaction data
            transactionEntity.setCustomerEntity(customerEntity);
            transactionEntity.setTotalPrice(req.getTotalPrice());
            transactionEntity.setTotalDiscount(req.getTotalDisc());
            transactionEntity.setSubtotal(req.getSubTotal());
            transactionRepository.save(transactionEntity);

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

            //Delete all product prices related to product id
            transactionDetailRepository.deleteAllByTransactionEntity_TransactionId(transactionId);

            //Insert all the transaction details
            Long lastTransactionDetailId = transactionDetailRepository.findFirstByOrderByTransactionDetailIdDesc().map(TransactionDetailEntity::getTransactionDetailId).orElse(0L);
            Long newTransactionDetailId = Generator.generateId(lastTransactionDetailId);

            for(TransactionDetailDTO dtos : req.getTransactionDetailDtos()){
                if(dtos != null) {
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
                }
            }
            return true;
        }catch (Exception e){
            System.out.println("Exception catched : " + e);
            return false;
        }
    }
}
