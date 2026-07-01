package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.TipeKartuStok;
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

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

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
    public ResponseInBoolean createTransaction(CreateTransactionRequest req, AccountEntity accountData, boolean isBranch) {
        String lastProduct = "Tanya Leon";
        TipeKartuStok tipeKartuStok = TipeKartuStok.PENJUALAN;
        if(isBranch){
            tipeKartuStok = TipeKartuStok.TRANSFER_STOK;
        }
        ClientEntity clientData = accountData.getClientEntity();
        try {
            //Get Customer Entity
            Optional<CustomerEntity> customerEntityOpt = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getCustomerId(), clientData.getClientId());
            if (customerEntityOpt.isEmpty()) {
                return new ResponseInBoolean(false, "Customer tidak ada");
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
            transactionEntity.setAccountEntity(accountData);
            transactionRepository.save(transactionEntity);

            System.out.println("=====START LOG ID : " + transactionEntity.getTransactionId() + "=======");

            for (TransactionDetailDTO dtos : req.getTransactionDetailDTOS()) {
                System.out.println("Part Number : " + dtos.getCode());
                System.out.println("Nama Barang : " + dtos.getName());
                System.out.println("Product Id : " + dtos.getProductId());

                //Get product Entity
                Optional<ProductEntity> productEntityOpt = productRepository.findFirstByProductIdAndDeletedAtIsNull(dtos.getProductId());
                if(productEntityOpt.isEmpty()){
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return new ResponseInBoolean(false, "Produk " + dtos.getName() + " tidak ditemukan");
                }
                ProductEntity productEntity = productEntityOpt.get();
                entityManager.refresh(productEntity);

                System.out.println("Produk: " + productEntity.getShortName() + "(" +productEntity.getStock() + ") VALID");
                lastProduct = dtos.getCode();
                TransactionDetailEntity transactionDetailEntity = new TransactionDetailEntity();
                transactionDetailEntity.setProductId(dtos.getProductId());
                transactionDetailEntity.setShortName(productEntity.getShortName());
                transactionDetailEntity.setFullName(productEntity.getFullName());
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
                stockMovementService.insertKartuStok(new AdjustStockDTO(
                        productEntity,
                        generatedNotaNumber,
                        tipeKartuStok,
                        0L,
                        dtos.getQty(),
                        newStock,
                        clientData,
                        getCurrentTimestamp()
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
            AccountEntity accountData,
            boolean isBranch) {

        TipeKartuStok tipeKartuStok = isBranch
                ? TipeKartuStok.KOREKSI_TRANSFER_STOK
                : TipeKartuStok.KOREKSI_PENJUALAN;

        String lastProduct = "-";
        ClientEntity clientData = accountData.getClientEntity();

        try {

            CustomerEntity customer = customerRepository
                    .findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(
                            req.getCustomerId(),
                            clientData.getClientId())
                    .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));

            TransactionEntity transaction = transactionRepository
                    .findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(
                            clientData.getClientId(), transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaksi tidak ditemukan"));

            transaction.setCustomerEntity(customer);
            transaction.setTotalPrice(req.getTotalPrice());
            transaction.setTotalDiscount(req.getTotalDisc());
            transaction.setSubtotal(req.getSubtotal());
            transaction.setAccountEntity(accountData);
            transactionRepository.save(transaction);

            List<TransactionDetailEntity> oldDetails =
                    transactionDetailRepository
                            .findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(transactionId);

            Map<Long, TransactionDetailEntity> oldMap = oldDetails.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            TransactionDetailEntity::getProductId,
                            java.util.function.Function.identity()));

            Map<Long, TransactionDetailDTO> newMap = req.getTransactionDetailDTOS().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            TransactionDetailDTO::getProductId,
                            java.util.function.Function.identity()));

            Set<Long> productIds = new HashSet<>();
            productIds.addAll(oldMap.keySet());
            productIds.addAll(newMap.keySet());

            Map<Long, ProductEntity> productCache = new HashMap<>();

            for (Long productId : productIds) {

                TransactionDetailEntity oldDetail = oldMap.get(productId);
                TransactionDetailDTO newDetail = newMap.get(productId);

                long oldQty = oldDetail == null ? 0 : oldDetail.getQty();
                long newQty = newDetail == null ? 0 : newDetail.getQty();

                long delta = newQty - oldQty;

                if (delta == 0) {
                    continue;
                }

                lastProduct = String.valueOf(productId);

                ProductEntity product = productRepository
                        .findFirstByProductIdAndDeletedAtIsNull(productId)
                        .orElseThrow(() -> new RuntimeException("Produk ID " + productId + " tidak ditemukan"));

                long newStock = product.getStock() - delta;

//                if (newStock < 0) {
//                    throw new RuntimeException(product.getFullName() + " stok tidak mencukupi");
//                }

                product.setStock(newStock);
                productRepository.save(product);

                productCache.put(productId, product);

                stockMovementService.insertKartuStok(
                        new AdjustStockDTO(
                                product,
                                transaction.getTransactionNumber(),
                                tipeKartuStok,
                                delta < 0 ? -delta : 0,
                                delta > 0 ? delta : 0,
                                newStock,
                                clientData,
                                getCurrentTimestamp()));
            }

            transactionDetailRepository.deleteAllByTransactionEntity_TransactionId(transactionId);

            for (TransactionDetailDTO dto : req.getTransactionDetailDTOS()) {

                ProductEntity product = productCache.get(dto.getProductId());

                if (product == null) {
                    product = productRepository
                            .findFirstByProductIdAndDeletedAtIsNull(dto.getProductId())
                            .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan"));
                }

                TransactionDetailEntity detail = new TransactionDetailEntity();
                detail.setTransactionEntity(transaction);
                detail.setProductId(product.getProductId());
                detail.setShortName(product.getShortName());
                detail.setFullName(product.getFullName());
                detail.setQty(dto.getQty());
                detail.setPrice(dto.getPrice());
                detail.setDiscountAmount(dto.getDiscAmount());
                detail.setTotalPrice(dto.getTotal());
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

//    @Transactional
//    public ResponseInBoolean editTransaction(
//            Long transactionId,
//            CreateTransactionRequest req,
//            AccountEntity accountData,
//            boolean isBranch
//    ) {
//        System.out.println("REQ ISINYA APA : " + req);
//        TipeKartuStok tipeKartuStok = TipeKartuStok.KOREKSI_PENJUALAN;
//        if(isBranch){
//            tipeKartuStok = TipeKartuStok.KOREKSI_TRANSFER_STOK;
//        }
//        String lastProduct = "-";
//        ClientEntity clientData = accountData.getClientEntity();
//        try {
//            // =========================
//            // 1. VALIDASI CUSTOMER
//            // =========================
//            CustomerEntity customer = customerRepository
//                    .findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(
//                            req.getCustomerId(),
//                            clientData.getClientId()
//                    )
//                    .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));
//
//            // =========================
//            // 2. VALIDASI TRANSAKSI
//            // =========================
//            TransactionEntity transaction = transactionRepository
//                    .findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(
//                            clientData.getClientId(),
//                            transactionId
//                    )
//                    .orElseThrow(() -> new RuntimeException("Transaksi tidak ditemukan"));
//
//            // =========================
//            // 3. UPDATE HEADER TRANSAKSI
//            // =========================
//            transaction.setCustomerEntity(customer);
//            transaction.setTotalPrice(req.getTotalPrice());
//            transaction.setTotalDiscount(req.getTotalDisc());
//            transaction.setSubtotal(req.getSubtotal());
//            transaction.setAccountEntity(accountData);
//            transactionRepository.save(transaction);
//
//            // =========================
//            // 4. MAP DETAIL LAMA
//            // =========================
//            List<TransactionDetailEntity> oldDetails =
//                    transactionDetailRepository.findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(transactionId);
//
//            Map<String, TransactionDetailEntity> oldDetailMap = new HashMap<>();
//            System.out.println("Getting the old detail map");
//            for (TransactionDetailEntity d : oldDetails) {
//                System.out.println("Product id ini : " + d.getProductId());
//                oldDetailMap.put(String.valueOf(d.getProductId()), d);
//            }
//            System.out.println("Old detail map : " + oldDetailMap.size());
//
//
//            // =========================
//            // 5. MAP DETAIL BARU
//            // =========================
//            System.out.println("Getting the new detail map");
//
//            Map<String, TransactionDetailDTO> newDetailMap = new HashMap<>();
//            for (TransactionDetailDTO dto : req.getTransactionDetailDTOS()) {
//                System.out.println("Product data yg ini apa : " + dto);
//                newDetailMap.put(String.valueOf(dto.getProductId()), dto);
//            }
//            System.out.println("New detail map : " + newDetailMap.size());
//
//            // =========================
//            // 6. UNION SEMUA PRODUK
//            // =========================
//            Set<String> allProductCodes = new HashSet<>();
//            allProductCodes.addAll(oldDetailMap.keySet());
//            allProductCodes.addAll(newDetailMap.keySet());
//
//            // =========================
//            // 7. LOOP PER PRODUK (DELTA)
//            // =========================
//            System.out.println("Start the loop");
//            for (String code : allProductCodes) {
//                System.out.println("Code : " + code);
//                TransactionDetailEntity oldDetail = oldDetailMap.get(code);
//                TransactionDetailDTO newDetail = newDetailMap.get(code);
//
//                long oldQty = oldDetail != null ? oldDetail.getQty() : 0;
//                long newQty = newDetail != null ? newDetail.getQty() : 0;
//
//                long delta = newQty - oldQty;
//
//                // Qty sama → SKIP TOTAL
//                if (delta == 0) {
//                    continue;
//                }
//
//                lastProduct = code;
//
//                // =========================
//                // 8. LOCK PRODUCT
//                // =========================
//                Long productId = newDetail != null ? newDetail.getProductId() : oldDetail.getProductId();
//                System.out.println("Start locking the product, with id : " + productId);
//
//                Optional<ProductEntity> productEntityOpt = productRepository.findFirstByProductIdAndDeletedAtIsNull(productId);
//                if(productEntityOpt.isEmpty()){
//                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                    return new ResponseInBoolean(false, "Produk " + newDetail.getName() + " tidak ditemukan");
//                }
//                ProductEntity product = productEntityOpt.get();
////                ProductEntity product = productRepository.findAndLockProduct(
////                        newDetail != null ? newDetail.getName() : oldDetail.getFullName(),
////                        code,
////                        clientData.getClientId()
////                );
//
//                if (product == null) {
//                    throw new RuntimeException("Produk " + code + " tidak ditemukan");
//                }
//                System.out.println("otwupdate stock");
//
//
//                // =========================
//                // 10. UPDATE STOCK
//                // =========================
//                long newStock = product.getStock() - delta;
//                product.setStock(newStock);
//                productRepository.save(product);
//
//                // =========================
//                // 11. KARTU STOCK
//                // =========================
//                stockMovementService.insertKartuStok(new AdjustStockDTO(
//                        product,
//                        transaction.getTransactionNumber(),
//                        tipeKartuStok,
//                        delta < 0 ? Math.abs(delta) : 0L,
//                        delta > 0 ? delta : 0L,
//                        newStock,
//                        clientData,
//                        getCurrentTimestamp()
//                ));
//            }
//
//            // =========================
//            // 12. REPLACE DETAIL TRANSAKSI
//            // =========================
//            transactionDetailRepository.deleteAllByTransactionEntity_TransactionId(transactionId);
//
//            System.out.println("otw loop ini");
//
//            for (TransactionDetailDTO dto : req.getTransactionDetailDTOS()) {
////                ProductEntity product = productRepository.findAndLockProduct(
////                        dto.getName(),
////                        dto.getCode(),
////                        clientData.getClientId()
////                );
//                System.out.println("otw get producdt lagi");
//
//                Optional<ProductEntity> productEntityOpt = productRepository.findFirstByProductIdAndDeletedAtIsNull(dto.getProductId());
//                if(productEntityOpt.isEmpty()){
//                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                    return new ResponseInBoolean(false, "Produk " + dto.getName() + " tidak ditemukan");
//                }
//                ProductEntity product = productEntityOpt.get();
//
//                TransactionDetailEntity detail = new TransactionDetailEntity();
//                detail.setProductId(product.getProductId());
//                detail.setShortName(product.getShortName());
//                detail.setFullName(product.getFullName());
//                detail.setQty(dto.getQty());
//                detail.setPrice(dto.getPrice());
//                detail.setDiscountAmount(dto.getDiscAmount());
//                detail.setTotalPrice(dto.getTotal());
//                detail.setTransactionEntity(transaction);
//                detail.setBasicPrice(product.getSupplierPrice());
//
//                BigDecimal totalBasic = product.getSupplierPrice()
//                        .multiply(BigDecimal.valueOf(dto.getQty()));
//                detail.setTotalProfit(dto.getTotal().subtract(totalBasic));
//
//                transactionDetailRepository.save(detail);
//            }
//
//            return new ResponseInBoolean(true, transaction.getTransactionNumber());
//
//        } catch (Exception e) {
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            return new ResponseInBoolean(false,
//                    e.getMessage() + " (ERROR di produk: " + lastProduct + ")");
//        }
//    }

}
