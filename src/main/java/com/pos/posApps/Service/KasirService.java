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
    public ResponseInBoolean editTransaction(
            Long transactionId,
            CreateTransactionRequest req,
            ClientEntity clientData
    ) {
        String lastProduct = "-";

        try {
            // =========================
            // 1. VALIDASI CUSTOMER
            // =========================
            CustomerEntity customer = customerRepository
                    .findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(
                            req.getCustomerId(),
                            clientData.getClientId()
                    )
                    .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));

            // =========================
            // 2. VALIDASI TRANSAKSI
            // =========================
            TransactionEntity transaction = transactionRepository
                    .findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(
                            clientData.getClientId(),
                            transactionId
                    )
                    .orElseThrow(() -> new RuntimeException("Transaksi tidak ditemukan"));

            // =========================
            // 3. UPDATE HEADER TRANSAKSI
            // =========================
            transaction.setCustomerEntity(customer);
            transaction.setTotalPrice(req.getTotalPrice());
            transaction.setTotalDiscount(req.getTotalDisc());
            transaction.setSubtotal(req.getSubtotal());
            transactionRepository.save(transaction);

            // =========================
            // 4. MAP DETAIL LAMA
            // =========================
            List<TransactionDetailEntity> oldDetails =
                    transactionDetailRepository.findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(transactionId);

            Map<String, TransactionDetailEntity> oldDetailMap = new HashMap<>();
            for (TransactionDetailEntity d : oldDetails) {
                oldDetailMap.put(d.getShortName(), d);
            }

            // =========================
            // 5. MAP DETAIL BARU
            // =========================
            Map<String, TransactionDetailDTO> newDetailMap = new HashMap<>();
            for (TransactionDetailDTO dto : req.getTransactionDetailDTOS()) {
                newDetailMap.put(dto.getCode(), dto);
            }

            // =========================
            // 6. UNION SEMUA PRODUK
            // =========================
            Set<String> allProductCodes = new HashSet<>();
            allProductCodes.addAll(oldDetailMap.keySet());
            allProductCodes.addAll(newDetailMap.keySet());

            // =========================
            // 7. LOOP PER PRODUK (DELTA)
            // =========================
            for (String code : allProductCodes) {

                TransactionDetailEntity oldDetail = oldDetailMap.get(code);
                TransactionDetailDTO newDetail = newDetailMap.get(code);

                long oldQty = oldDetail != null ? oldDetail.getQty() : 0;
                long newQty = newDetail != null ? newDetail.getQty() : 0;

                long delta = newQty - oldQty;

                // Qty sama → SKIP TOTAL
                if (delta == 0) {
                    continue;
                }

                lastProduct = code;

                // =========================
                // 8. LOCK PRODUCT
                // =========================
                ProductEntity product = productRepository.findAndLockProduct(
                        newDetail != null ? newDetail.getName() : oldDetail.getFullName(),
                        code,
                        clientData.getClientId()
                );

                if (product == null) {
                    throw new RuntimeException("Produk " + code + " tidak ditemukan");
                }

                // =========================
                // 9. VALIDASI STOCK
                // =========================
                if (delta > 0 && product.getStock() < delta) {
                    throw new RuntimeException(
                            "Stock produk " + code + " tidak mencukupi. Sisa: " + product.getStock()
                    );
                }

                // =========================
                // 10. UPDATE STOCK
                // =========================
                long newStock = product.getStock() - delta;
                product.setStock(newStock);
                productRepository.save(product);

                // =========================
                // 11. KARTU STOCK
                // =========================
                stockMovementService.insertKartuStok(new AdjustStockDTO(
                        product,
                        transaction.getTransactionNumber(),
                        TipeKartuStok.KOREKSI_PENJUALAN,
                        delta < 0 ? Math.abs(delta) : 0L,
                        delta > 0 ? delta : 0L,
                        newStock,
                        clientData
                ));
            }

            // =========================
            // 12. REPLACE DETAIL TRANSAKSI
            // =========================
            transactionDetailRepository.deleteAllByTransactionEntity_TransactionId(transactionId);

            for (TransactionDetailDTO dto : req.getTransactionDetailDTOS()) {
                ProductEntity product = productRepository.findAndLockProduct(
                        dto.getName(),
                        dto.getCode(),
                        clientData.getClientId()
                );

                TransactionDetailEntity detail = new TransactionDetailEntity();
                detail.setShortName(dto.getCode());
                detail.setFullName(dto.getName());
                detail.setQty(dto.getQty());
                detail.setPrice(dto.getPrice());
                detail.setDiscountAmount(dto.getDiscAmount());
                detail.setTotalPrice(dto.getTotal());
                detail.setTransactionEntity(transaction);
                detail.setBasicPrice(product.getSupplierPrice());

                BigDecimal totalBasic = product.getSupplierPrice()
                        .multiply(BigDecimal.valueOf(dto.getQty()));
                detail.setTotalProfit(dto.getTotal().subtract(totalBasic));

                transactionDetailRepository.save(detail);
            }

            return new ResponseInBoolean(true, transaction.getTransactionNumber());

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false,
                    e.getMessage() + " (ERROR di produk: " + lastProduct + ")");
        }
    }

}
