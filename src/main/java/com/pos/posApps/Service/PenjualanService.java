package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class PenjualanService {
    @Autowired
    TransactionDetailRepository transactionDetailRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ProductRepository productRepository;

    public List<PenjualanDTO> getPenjualanData(Long clientId, LocalDateTime startDate, LocalDateTime endDate, Long customerId) {
        List<TransactionEntity> transactionData;
        if(customerId == null){
            transactionData = transactionRepository.findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate);
        }else{
            transactionData = transactionRepository.findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(clientId, customerId, startDate, endDate);
        }
        return transactionData.stream().map(transactions -> new PenjualanDTO(
                transactions.getTransactionId(),
                new CustomerDTO(
                        transactions.getCustomerEntity().getCustomerId(),
                        transactions.getCustomerEntity().getName()
                ),
                transactions.getTransactionNumber(),
                transactions.getSubtotal(),
                transactions.getTotalPrice(),
                transactions.getTotalDiscount(),
                transactions.getCreatedAt(),
                transactions.getTransactionDetailEntities().stream()
                        .map(transactionDetail -> new TransactionDetailDTO(
                                transactionDetail.getShortName(),
                                transactionDetail.getFullName(),
                                transactionDetail.getPrice(),
                                transactionDetail.getQty(),
                                transactionDetail.getDiscountAmount(),
                                transactionDetail.getTotalPrice()
                        ))
                        .collect(Collectors.toList())  // collect the stream to a list
        )).collect(Collectors.toList());
    }

    public PenjualanDTO getPenjualanDataById(Long clientId, Long penjualanId) {
        TransactionEntity transactions = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, penjualanId);
        return new PenjualanDTO(
                transactions.getTransactionId(),
                new CustomerDTO(
                        transactions.getCustomerEntity().getCustomerId(),
                        transactions.getCustomerEntity().getName()
                ),
                transactions.getTransactionNumber(),
                transactions.getSubtotal(),
                transactions.getTotalPrice(),
                transactions.getTotalDiscount(),
                transactions.getCreatedAt(),
                transactions.getTransactionDetailEntities().stream()
                        .map(transactionDetail -> new TransactionDetailDTO(
                                transactionDetail.getShortName(),
                                transactionDetail.getFullName(),
                                transactionDetail.getPrice(),
                                transactionDetail.getQty(),
                                transactionDetail.getDiscountAmount(),
                                transactionDetail.getTotalPrice()
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
}
