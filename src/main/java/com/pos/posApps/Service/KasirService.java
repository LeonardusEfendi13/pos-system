package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Service
public class KasirService {
    @PersistenceContext
    private EntityManager entityManager;

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

    @Autowired
    DailyCounterRepository dailyCounterRepository;

    @Transactional
    public String generateTodayNota(Long clientId) {
        String todayStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        LocalDate today = LocalDate.now();

        Optional<DailyCounterEntity> counterOpt = dailyCounterRepository.findByCounterDateForUpdate(today);

        long counter;

        if (counterOpt.isPresent()) {
            DailyCounterEntity dailyCounter = counterOpt.get();
            counter = dailyCounter.getLastCounter() + 1;
            dailyCounter.setLastCounter(counter);
            dailyCounterRepository.save(dailyCounter);
        } else {
            counter = 1L;
            DailyCounterEntity newCounter = new DailyCounterEntity();
            newCounter.setCounterDate(today);
            newCounter.setLastCounter(counter);
            dailyCounterRepository.save(newCounter);
        }

        return todayStr + String.format("%03d", counter);
    }

    @Transactional
    public ResponseInBoolean createTransaction(CreateTransactionRequest req, ClientEntity clientData) {
        String lastProduct = "Tanya Leon";
        try {
            //Get Customer Entity
            Optional<CustomerEntity> customerEntityOpt = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getCustomerId(), clientData.getClientId());
            if (customerEntityOpt.isEmpty()) {
                return new ResponseInBoolean(true, "Customer tidak ada");
            }

            CustomerEntity customerEntity = customerEntityOpt.get();
            String generatedNotaNumber = generateTodayNota(clientData.getClientId());

            //insert the transaction data
            TransactionEntity transactionEntity = new TransactionEntity();
            transactionEntity.setClientEntity(clientData);
            transactionEntity.setTransactionNumber(generatedNotaNumber);
            transactionEntity.setCustomerEntity(customerEntity);
            transactionEntity.setTotalPrice(req.getTotalPrice());
            transactionEntity.setTotalDiscount(req.getTotalDisc());
            transactionEntity.setSubtotal(req.getSubtotal());
            transactionRepository.save(transactionEntity);

            System.out.println("=====START LOG ID : " + transactionEntity.getTransactionId() + "=======");

            for (TransactionDetailDTO dtos : req.getTransactionDetailDTOS()) {
                System.out.println("Part Number : " + dtos.getCode());
                System.out.println("Nama Barang : " + dtos.getName());

                //Get product Entity
//                ProductEntity productEntity = productRepository.findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(dtos.getName(), dtos.getCode(), clientData.getClientId());
                ProductEntity productEntity = productRepository.findAndLockProduct(dtos.getName(), dtos.getCode(), clientData.getClientId());
                entityManager.refresh(productEntity);

                if (productEntity == null) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return new ResponseInBoolean(true, "Produk " + dtos.getName() + " tidak ditemukan");
                }
                System.out.println("Produk: " + productEntity.getShortName() + "(" +productEntity.getStock() + ") VALID");
                lastProduct = dtos.getCode();
                TransactionDetailEntity transactionDetailEntity = new TransactionDetailEntity();
                transactionDetailEntity.setShortName(dtos.getCode());
                transactionDetailEntity.setFullName(dtos.getName());
                transactionDetailEntity.setQty(dtos.getQty());
                transactionDetailEntity.setPrice(dtos.getPrice());
                transactionDetailEntity.setDiscountAmount(dtos.getDiscAmount());
                transactionDetailEntity.setTotalPrice(dtos.getTotal());
                transactionDetailEntity.setTransactionEntity(transactionEntity);
                transactionDetailEntity.setBasicPrice(productEntity.getSupplierPrice());
                BigDecimal totalBasicPrice = productEntity.getSupplierPrice().multiply(BigDecimal.valueOf(dtos.getQty()));
                BigDecimal totalProfit = dtos.getTotal().subtract(totalBasicPrice);
                transactionDetailEntity.setTotalProfit(totalProfit);
                transactionDetailRepository.save(transactionDetailEntity);

                //Update product stock
                System.out.println("Stock Before : " + productEntity.getStock());
                System.out.println("Qty : " + dtos.getQty());
                Long newStock = productEntity.getStock() - dtos.getQty();
                productEntity.setStock(newStock);
                productRepository.save(productEntity);
//                List<Long> newStockAfterUpdated = productRepository.reduceStockReturning(productEntity.getProductId(),dtos.getQty());
//                System.out.println("Updated size : " + newStockAfterUpdated.size());
//                if (newStockAfterUpdated.isEmpty()) {
//                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                    return new ResponseInBoolean(false, "Stock produk " + dtos.getName() + " tidak mencukupi atau sedang dipakai transaksi lain");
//                }
//                Long newStockValues = newStockAfterUpdated.get(0);


                stockMovementService.insertKartuStok(new AdjustStockDTO(
                        productEntity,
                        generatedNotaNumber,
                        TipeKartuStok.PENJUALAN,
                        0L,
                        dtos.getQty(),
                        newStock,
                        clientData
                ));
                System.out.println("Stock After : " + newStock);
                System.out.println();
            }
            System.out.println("=====END LOG=======");
            System.out.println();
            return new ResponseInBoolean(true, generatedNotaNumber);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage() + ". ERROR KARENA : " + lastProduct);
        }
    }

    @Transactional
    public ResponseInBoolean editTransaction(Long transactionId, CreateTransactionRequest req, ClientEntity clientData) {
        String lastProduct = "Tanya Leon";
        try {
            // Validate customer
            Optional<CustomerEntity> customerEntityOpt = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getCustomerId(), clientData.getClientId());
            if (customerEntityOpt.isEmpty()) {
                return new ResponseInBoolean(true, "Customer tidak ada");
            }
            CustomerEntity customerEntity = customerEntityOpt.get();

            // Validate transaction
            Optional<TransactionEntity> transactionEntityOpt = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(clientData.getClientId(), transactionId);
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
//                ProductEntity product = productRepository.findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(old.getFullName(), old.getShortName(), clientData.getClientId());
                ProductEntity product = productRepository.findAndLockProduct(old.getFullName(), old.getShortName(), clientData.getClientId());
                entityManager.refresh(product);
                if (product != null) {
                    String key = old.getShortName();
                    Long newQty = newQtyMap.getOrDefault(key, null);

                    boolean isQtyChanged = (newQty == null || !Objects.equals(newQty, old.getQty()));
                    if (!isQtyChanged) {
                        oldProductMap.put(key, old);
                        continue; // Skip kartu stok restore
                    }

                    // Restore stock
                    product.setStock(product.getStock() + old.getQty());
                    productRepository.save(product);

                    // Insert kartu stok restore karena qty berubah
                    stockMovementService.insertKartuStok(new AdjustStockDTO(
                            product,
                            transactionEntity.getTransactionNumber(),
                            TipeKartuStok.KOREKSI_PENJUALAN,
                            old.getQty(),
                            0L,
                            product.getStock(),
                            clientData
                    ));
                    oldProductMap.put(key, old);
                }
            }

            // Delete old transaction details
            transactionDetailRepository.deleteAllByTransactionEntity_TransactionId(transactionId);

            // Track product codes from new transaction
            Set<String> newProductKeys = new HashSet<>();

            for (TransactionDetailDTO dto : req.getTransactionDetailDTOS()) {
                if (dto == null) continue;

//                ProductEntity product = productRepository.findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(dto.getName(), dto.getCode(), clientData.getClientId());
                ProductEntity product = productRepository.findAndLockProduct(dto.getName(), dto.getCode(), clientData.getClientId());
                entityManager.refresh(product);
                if (product == null) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return new ResponseInBoolean(true, "Produk " + dto.getName() + " tidak ditemukan");
                }

                String key = dto.getCode();
                newProductKeys.add(key);

                // Save new transaction detail
                TransactionDetailEntity transactionDetailEntity = new TransactionDetailEntity();
                lastProduct = dto.getCode();
                transactionDetailEntity.setShortName(dto.getCode());
                transactionDetailEntity.setFullName(dto.getName());
                transactionDetailEntity.setQty(dto.getQty());
                transactionDetailEntity.setPrice(dto.getPrice());
                transactionDetailEntity.setDiscountAmount(dto.getDiscAmount());
                transactionDetailEntity.setTotalPrice(dto.getTotal());
                transactionDetailEntity.setTransactionEntity(transactionEntity);
                transactionDetailEntity.setBasicPrice(product.getSupplierPrice());
                BigDecimal totalBasicPrice = product.getSupplierPrice().multiply(BigDecimal.valueOf(dto.getQty()));
                BigDecimal totalProfit = dto.getTotal().subtract(totalBasicPrice);
                transactionDetailEntity.setTotalProfit(totalProfit);
                transactionDetailRepository.save(transactionDetailEntity);

                Long updatedStock = product.getStock() - dto.getQty();
                product.setStock(updatedStock);
                productRepository.save(product);

                // Cek apakah qty berubah dibanding transaksi lama
                TransactionDetailEntity oldDetail = oldProductMap.get(dto.getCode());
                if (oldDetail != null && Objects.equals(oldDetail.getQty(), dto.getQty())) {
                    // Qty sama → tidak perlu insert kartu stok
                    continue;
                }

                // Qty berubah atau produk baru → insert kartu stok
                stockMovementService.insertKartuStok(new AdjustStockDTO(
                        product,
                        transactionEntity.getTransactionNumber(),
                        TipeKartuStok.KOREKSI_PENJUALAN,
                        0L,
                        dto.getQty(),
                        updatedStock,
                        clientData
                ));
            }
            return new ResponseInBoolean(true, transactionEntity.getTransactionNumber());

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage() + ". ERROR karena : " + lastProduct);
        }
    }
}
