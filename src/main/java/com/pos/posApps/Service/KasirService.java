package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.pos.posApps.Util.Generator.*;

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

    @Autowired
    StockMovementService stockMovementService;

    @Transactional
    public ResponseInBoolean createTransaction(CreateTransactionRequest req, ClientEntity clientData) {
        try {
            //Get Customer Entity
            Optional<CustomerEntity> customerEntityOpt = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getCustomerId(), clientData.getClientId());
            if (customerEntityOpt.isEmpty()) {
                return new ResponseInBoolean(true, "Customer tidak ada");
            }

            CustomerEntity customerEntity = customerEntityOpt.get();
            //Get last Transaction id
            Long lastTransactionId = transactionRepository.findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByTransactionIdDesc(clientData.getClientId()).map(TransactionEntity::getTransactionId).orElse(0L);
            Long newTransactionId = Generator.generateId(lastTransactionId);
            String generatedNotaNumber = generateNotaNumber(newTransactionId);

            //insert the transaction data
            TransactionEntity transactionEntity = new TransactionEntity();
            transactionEntity.setClientEntity(clientData);
            transactionEntity.setTransactionNumber(generatedNotaNumber);
            transactionEntity.setTransactionId(newTransactionId);
            transactionEntity.setCustomerEntity(customerEntity);
            transactionEntity.setTotalPrice(req.getTotalPrice());
            transactionEntity.setTotalDiscount(req.getTotalDisc());
            transactionEntity.setSubtotal(req.getSubtotal());
            transactionRepository.save(transactionEntity);

            System.out.println("Created transaction : " + newTransactionId);


            //Insert all the transaction details
            Long lastTransactionDetailId = transactionDetailRepository.findFirstByDeletedAtIsNullOrderByTransactionDetailIdDesc().map(TransactionDetailEntity::getTransactionDetailId).orElse(0L);
            Long newTransactionDetailId = Generator.generateId(lastTransactionDetailId);

            for (TransactionDetailDTO dtos : req.getTransactionDetailDTOS()) {
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

                boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                        productEntity,
                        generatedNotaNumber,
                        TipeKartuStok.PENJUALAN,
                        0L,
                        dtos.getQty(),
                        newStock,
                        clientData
                ));
                if (!isAdjusted) {
                    System.out.println("Gagal adjust di edit product");
                    return new ResponseInBoolean(false, "Gagal adjust di create transaction");
                }
            }
            return new ResponseInBoolean(true, generatedNotaNumber);
        } catch (Exception e) {
            System.out.println("Exception catched : " + e);
            return new ResponseInBoolean(false, e.getMessage());
        }
    }

    @Transactional
    public ResponseInBoolean editTransaction(Long transactionId, CreateTransactionRequest req, ClientEntity clientData) {
        try {
            //Get Customer Entity
            Optional<CustomerEntity> customerEntityOpt = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getCustomerId(), clientData.getClientId());
            if (customerEntityOpt.isEmpty()) {
                return new ResponseInBoolean(true, "Customer tidak ada");
            }
            CustomerEntity customerEntity = customerEntityOpt.get();

            //Check if transaction exist
            Optional<TransactionEntity> transactionEntityOpt = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNull(clientData.getClientId(), transactionId);
            if (transactionEntityOpt.isEmpty()) {
                System.out.println("transaction ga nemu");
                return new ResponseInBoolean(false, "Data transaksi tidak ditemukan");
            }
            TransactionEntity transactionEntity = transactionEntityOpt.get();

            //insert the transaction data
            transactionEntity.setCustomerEntity(customerEntity);
            transactionEntity.setTotalPrice(req.getTotalPrice());
            transactionEntity.setTotalDiscount(req.getTotalDisc());
            transactionEntity.setSubtotal(req.getSubtotal());
            transactionRepository.save(transactionEntity);

            //Restore stock from old transaction
            List<TransactionDetailEntity> oldTransactions = transactionDetailRepository.findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(transactionId);
            for (TransactionDetailEntity old : oldTransactions) {
                ProductEntity product = productRepository.findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(old.getFullName(), old.getShortName(), clientData.getClientId());
                if (product != null) {
                    Long restoredStock = product.getStock() + old.getQty();
                    product.setStock(restoredStock);
                    productRepository.save(product);


                    System.out.println("Otw insert kartu stok di restoring product stock di edit penjualan");
                    if (!Objects.equals(product.getStock(), old.getQty())) {
                        boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                                product,
                                transactionEntity.getTransactionNumber(),
                                TipeKartuStok.KOREKSI_PENJUALAN,
                                old.getQty(),
                                0L,
                                restoredStock,
                                clientData
                        ));
                        if (!isAdjusted) {
                            System.out.println("Gagal adjust di edit penjualan (1)");
                            return new ResponseInBoolean(false, "Gagal adjust di create transaction");
                        }
                        System.out.println("Sukses adjust di edit penjualan (1)");
                    }
                }
            }

            //Delete all product prices related to product id
            transactionDetailRepository.deleteAllByTransactionEntity_TransactionId(transactionId);

            //Insert all the transaction details
            Long lastTransactionDetailId = transactionDetailRepository.findFirstByDeletedAtIsNullOrderByTransactionDetailIdDesc().map(TransactionDetailEntity::getTransactionDetailId).orElse(0L);
            Long newTransactionDetailId = Generator.generateId(lastTransactionDetailId);

            for (TransactionDetailDTO dtos : req.getTransactionDetailDTOS()) {
                if (dtos != null) {
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

                    // cari di snapshot lama
                    TransactionDetailEntity oldDetail = oldTransactions.stream()
                            .filter(o -> o.getShortName().equals(dtos.getCode()) || o.getFullName().equals(dtos.getName()))
                            .findFirst()
                            .orElse(null);

                    if (oldDetail != null && Objects.equals(oldDetail.getQty(), dtos.getQty())) {
                        System.out.println("Qty tidak berubah, skip kartu stok untuk product " + dtos.getName());
                        continue;
                    }
                    boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                            productEntity,
                            transactionEntity.getTransactionNumber(),
                            TipeKartuStok.KOREKSI_PENJUALAN,
                            0L,
                            dtos.getQty(),
                            newStock,
                            clientData
                    ));
                    if (!isAdjusted) {
                        System.out.println("Gagal adjust di edit product (2)");
                        return new ResponseInBoolean(false, "Gagal adjust di create transaction (2)");
                    }
                }
            }
            return new ResponseInBoolean(true, "Data berhasil disimpan");
        } catch (Exception e) {
            System.out.println("Exception catched : " + e);
            return new ResponseInBoolean(false, e.getMessage());
        }
    }
}
