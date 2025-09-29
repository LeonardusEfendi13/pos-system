package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
            // Validate customer
            Optional<CustomerEntity> customerEntityOpt = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getCustomerId(), clientData.getClientId());
            if (customerEntityOpt.isEmpty()) {
                return new ResponseInBoolean(true, "Customer tidak ada");
            }
            CustomerEntity customerEntity = customerEntityOpt.get();

            // Validate transaction
            Optional<TransactionEntity> transactionEntityOpt = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNull(clientData.getClientId(), transactionId);
            if (transactionEntityOpt.isEmpty()) {
                return new ResponseInBoolean(false, "Data transaksi tidak ditemukan");
            }
            TransactionEntity transactionEntity = transactionEntityOpt.get();

            // Update transaction summary
            transactionEntity.setCustomerEntity(customerEntity);
            transactionEntity.setTotalPrice(req.getTotalPrice());
            transactionEntity.setTotalDiscount(req.getTotalDisc());
            transactionEntity.setSubtotal(req.getSubtotal());
            transactionRepository.save(transactionEntity);

            // Build map of new transaction detail qty for comparison
            Map<String, Long> newQtyMap = new HashMap<>();
            for (TransactionDetailDTO dto : req.getTransactionDetailDTOS()) {
                if (dto != null) {
                    newQtyMap.put(dto.getCode(), dto.getQty());
                }
            }

            // Restore stock from old transaction (with qty comparison)
            List<TransactionDetailEntity> oldTransactions = transactionDetailRepository.findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(transactionId);
            Map<String, TransactionDetailEntity> oldProductMap = new HashMap<>();

            for (TransactionDetailEntity old : oldTransactions) {
                ProductEntity product = productRepository.findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(old.getFullName(), old.getShortName(), clientData.getClientId());
                if (product != null) {
                    String key = old.getShortName();
                    Long newQty = newQtyMap.getOrDefault(key, null);

                    boolean isQtyChanged = (newQty == null || !Objects.equals(newQty, old.getQty()));
                    if (!isQtyChanged) {
                        System.out.println("Qty tidak berubah untuk " + key + ", skip kartu stok restore.");
                        oldProductMap.put(key, old);
                        continue; // Skip kartu stok restore
                    }

                    // Restore stock
                    product.setStock(product.getStock() + old.getQty());
                    productRepository.save(product);

                    // Insert kartu stok restore karena qty berubah
                    boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                            product,
                            transactionEntity.getTransactionNumber(),
                            TipeKartuStok.KOREKSI_PENJUALAN,
                            old.getQty(),
                            0L,
                            product.getStock(),
                            clientData
                    ));
                    if (!isAdjusted) {
                        return new ResponseInBoolean(false, "Gagal adjust saat restore stok");
                    }

                    oldProductMap.put(key, old);
                }
            }

            // Delete old transaction details
            transactionDetailRepository.deleteAllByTransactionEntity_TransactionId(transactionId);

            // Track product codes from new transaction
            Set<String> newProductKeys = new HashSet<>();
            Long lastTransactionDetailId = transactionDetailRepository.findFirstByDeletedAtIsNullOrderByTransactionDetailIdDesc()
                    .map(TransactionDetailEntity::getTransactionDetailId).orElse(0L);
            Long newTransactionDetailId = Generator.generateId(lastTransactionDetailId);

            for (TransactionDetailDTO dto : req.getTransactionDetailDTOS()) {
                if (dto == null) continue;

                String key = dto.getCode();
                newProductKeys.add(key);

                // Save new transaction detail
                TransactionDetailEntity transactionDetailEntity = new TransactionDetailEntity();
                transactionDetailEntity.setTransactionDetailId(newTransactionDetailId);
                transactionDetailEntity.setShortName(dto.getCode());
                transactionDetailEntity.setFullName(dto.getName());
                transactionDetailEntity.setQty(dto.getQty());
                transactionDetailEntity.setPrice(dto.getPrice());
                transactionDetailEntity.setDiscountAmount(dto.getDiscAmount());
                transactionDetailEntity.setTotalPrice(dto.getTotal());
                transactionDetailEntity.setTransactionEntity(transactionEntity);
                transactionDetailRepository.save(transactionDetailEntity);
                newTransactionDetailId = Generator.generateId(newTransactionDetailId);

                // Update stock
                ProductEntity product = productRepository.findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(dto.getName(), dto.getCode(), clientData.getClientId());
                if (product == null) continue;

                Long updatedStock = product.getStock() - dto.getQty();
                product.setStock(updatedStock);
                productRepository.save(product);

                // Cek apakah qty berubah dibanding transaksi lama
                TransactionDetailEntity oldDetail = oldProductMap.get(dto.getCode());
                if (oldDetail != null && Objects.equals(oldDetail.getQty(), dto.getQty())) {
                    // Qty sama → tidak perlu insert kartu stok
                    System.out.println("Qty tidak berubah, skip kartu stok untuk " + dto.getName());
                    continue;
                }

                // Qty berubah atau produk baru → insert kartu stok
                boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                        product,
                        transactionEntity.getTransactionNumber(),
                        TipeKartuStok.KOREKSI_PENJUALAN,
                        0L,
                        dto.getQty(),
                        updatedStock,
                        clientData
                ));
                if (!isAdjusted) {
                    return new ResponseInBoolean(false, "Gagal adjust stok saat insert detail");
                }
            }

            return new ResponseInBoolean(true, transactionEntity.getTransactionNumber());

        } catch (Exception e) {
            System.out.println("Exception caught: " + e);
            return new ResponseInBoolean(false, e.getMessage());
        }
    }
}
