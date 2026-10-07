package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import com.pos.posApps.Util.PenjualanHistoryCustomerFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;
import static com.pos.posApps.Util.PenjualanHistoryCustomerFilter.resolve;

@Service
public class PenjualanService {
    @Autowired
    TransactionDetailRepository transactionDetailRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    StockMovementService stockMovementService;

    @Autowired
    BuktiBayarRepository buktiBayarRepository;

    @Autowired
    PaymentMethodService paymentMethodService;

    public BigDecimal getTotalRevenues(Long clientId) {
        LocalDateTime startDate = LocalDate.now().atStartOfDay();
        LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);
//        List<TransactionEntity> transactionData = transactionRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate).stream().toList();
        List<TransactionEntity> transactionData = transactionRepository.findFilteredTransactions(clientId, startDate, endDate).stream().toList();
        return transactionData.stream()
                .map(TransactionEntity::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public List<PenjualanDTO> getLast10Transaction(Long clientId) {
        LocalDateTime startDate = LocalDate.now().atStartOfDay();
        LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);
//        For testing only
//        LocalDateTime startDate = LocalDate.parse("2025-09-13").atStartOfDay();
//        LocalDateTime endDate = LocalDate.parse("2025-09-13").atTime(23, 59, 59);
//        List<TransactionEntity> transactionData = transactionRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate).stream().limit(20).toList();
        List<TransactionEntity> transactionData = transactionRepository.findFilteredTransactions(clientId, startDate, endDate).stream().limit(20).toList();

        return transactionData.stream().map(transactions -> new PenjualanDTO(
                transactions.getTransactionId(),
                new CustomerDTO(
                        transactions.getCustomerEntity().getCustomerId(),
                        transactions.getCustomerEntity().getName(),
                        transactions.getCustomerEntity().getAlamat()
                ),
                transactions.getTransactionNumber(),
                transactions.getSubtotal(),
                transactions.getTotalPrice(),
                transactions.getTotalDiscount(),
                transactions.getCreatedAt(),
                transactions.getTransactionDetailEntities().stream()
                        .map(transactionDetail -> new TransactionDetailDTO(
                                transactionDetail.getProductId(),
                                transactionDetail.getShortName(),
                                transactionDetail.getFullName(),
                                transactionDetail.getPrice(),
                                transactionDetail.getQty(),
                                transactionDetail.getDiscountAmount(),
                                transactionDetail.getTotalPrice(),
                                transactionDetail.getTotalProfit(),
                                transactionDetail.getBasicPrice()
                        ))
                        .collect(Collectors.toList()),
                transactions.getAccountEntity().getName(),
                transactions.isCash(),
                transactions.isPaid(),
                transactions.getPaidAmount(),
                transactions.getDueDate(),
                transactions.getPaymentMethodId(),
                transactions.getPaymentMethodName(),
                transactions.getPaymentMethodType(),
                transactions.getPaymentMethodRekening()

        )).collect(Collectors.toList());
    }

    // File: PenjualanService.java
    public Page<PenjualanDTO> getPenjualanData(Long clientId, LocalDateTime startDate, LocalDateTime endDate, List<Long> customerId, Pageable pageable) {
        Page<TransactionEntity> transactionData;
        if (customerId == null || customerId.isEmpty()) {
            transactionData = transactionRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate, pageable);
        } else {
            transactionData = transactionRepository.findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdInAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(clientId, customerId, startDate, endDate, pageable);
        }
        return transactionData.map(this::convertToDTO);
    }

    public Page<PenjualanDTO> getPenjualanHistoryData(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long selectedCustomerId,
            List<Long> cabangCustomerIds,
            Pageable pageable) {
        PenjualanHistoryCustomerFilter.ResolvedType resolved = resolve(selectedCustomerId, cabangCustomerIds);

        return switch (resolved.mode()) {
            case EMPTY -> Page.empty(pageable);
            case ALL -> getPenjualanData(clientId, startDate, endDate, List.of(), pageable);
            case INCLUDE -> getPenjualanData(clientId, startDate, endDate, resolved.customerIds(), pageable);
            case EXCLUDE -> transactionRepository
                    .findExcludingCustomers(
                            clientId,
                            resolved.customerIds(),
                            startDate,
                            endDate,
                            pageable)
                    .map(this::convertToDTO);
        };
    }

    public Page<PenjualanDTO> searchPenjualanData( Long clientId, LocalDateTime startDate, LocalDateTime endDate, List<Long> customerId, String search, Pageable pageable) {
        String trimmedSearch = (search != null) ? search.trim() : "";

        if (trimmedSearch.isEmpty()) {
            return getPenjualanData(clientId, startDate, endDate, customerId, pageable);
        }

        if(customerId.isEmpty()){
            customerId =  null;
        }

        Page<TransactionEntity> transactionData = transactionRepository
                .searchTransactions(
                        clientId,
                        startDate,
                        endDate,
                        customerId,
                        trimmedSearch,
                        pageable
                );

        return transactionData.map(this::convertToDTO);
    }

    public Page<PenjualanDTO> searchPenjualanHistoryData(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long selectedCustomerId,
            List<Long> cabangCustomerIds,
            String search,
            Pageable pageable) {
        String trimmedSearch = (search != null) ? search.trim() : "";

        if (trimmedSearch.isEmpty()) {
            return getPenjualanHistoryData(
                    clientId,
                    startDate,
                    endDate,
                    selectedCustomerId,
                    cabangCustomerIds,
                    pageable);
        }

        PenjualanHistoryCustomerFilter.ResolvedType resolved = resolve(selectedCustomerId, cabangCustomerIds);

        return switch (resolved.mode()) {
            case EMPTY -> Page.empty(pageable);
            case ALL -> searchPenjualanData(clientId, startDate, endDate, List.of(), trimmedSearch, pageable);
            case INCLUDE -> searchPenjualanData(
                    clientId,
                    startDate,
                    endDate,
                    resolved.customerIds(),
                    trimmedSearch,
                    pageable);
            case EXCLUDE -> transactionRepository
                    .searchTransactionsExcludingCustomers(
                            clientId,
                            startDate,
                            endDate,
                            resolved.customerIds(),
                            trimmedSearch,
                            pageable)
                    .map(this::convertToDTO);
        };
    }

    private PenjualanDTO convertToDTO(TransactionEntity transactions) {
        String name = "Unknown";
        if(transactions.getAccountEntity() != null) {
            if (transactions.getAccountEntity().getName() != null && !transactions.getAccountEntity().getName().isBlank()) {
                name = transactions.getAccountEntity().getName();
            }
        }
        return new PenjualanDTO(
                transactions.getTransactionId(),
                new CustomerDTO(
                        transactions.getCustomerEntity().getCustomerId(),
                        transactions.getCustomerEntity().getName(),
                        transactions.getCustomerEntity().getAlamat()
                ),
                transactions.getTransactionNumber(),
                transactions.getSubtotal(),
                transactions.getTotalPrice(),
                transactions.getTotalDiscount(),
                transactions.getCreatedAt(),
                transactions.getTransactionDetailEntities().stream()
                        .map(transactionDetail -> new TransactionDetailDTO(
                                transactionDetail.getProductId(),
                                transactionDetail.getShortName(),
                                transactionDetail.getFullName(),
                                transactionDetail.getPrice(),
                                transactionDetail.getQty(),
                                transactionDetail.getDiscountAmount(),
                                transactionDetail.getTotalPrice(),
                                transactionDetail.getTotalProfit(),
                                transactionDetail.getBasicPrice()
                        ))
                        .collect(Collectors.toList()),
                name,
                transactions.isCash(),
                transactions.isPaid(),
                transactions.getPaidAmount(),
                transactions.getDueDate(),
                transactions.getPaymentMethodId(),
                transactions.getPaymentMethodName(),
                transactions.getPaymentMethodType(),
                transactions.getPaymentMethodRekening()
        );
    }

    @Transactional
    public PenjualanDTO getPenjualanDataById(Long clientId, Long penjualanId) {
        Optional<TransactionEntity> transactionsOpt = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(clientId, penjualanId);
        if (transactionsOpt.isEmpty()) {
            return null;
        }
        TransactionEntity transactions = transactionsOpt.get();
        CustomerEntity customer = transactions.getCustomerEntity();
        List<TransactionDetailDTO> details = new ArrayList<>();
        for (TransactionDetailEntity transactionDetail : transactions.getTransactionDetailEntities()) {
            Long productId = resolveDetailProductId(clientId, transactionDetail);
            details.add(new TransactionDetailDTO(
                    productId,
                    transactionDetail.getShortName(),
                    transactionDetail.getFullName(),
                    transactionDetail.getPrice(),
                    transactionDetail.getQty(),
                    transactionDetail.getDiscountAmount(),
                    transactionDetail.getTotalPrice(),
                    transactionDetail.getTotalProfit(),
                    transactionDetail.getBasicPrice()
            ));
        }
        return new PenjualanDTO(
                transactions.getTransactionId(),
                new CustomerDTO(
                        customer == null ? null : customer.getCustomerId(),
                        customer == null ? "" : customer.getName(),
                        customer == null ? "" : customer.getAlamat()
                ),
                transactions.getTransactionNumber(),
                transactions.getSubtotal(),
                transactions.getTotalPrice(),
                transactions.getTotalDiscount(),
                transactions.getCreatedAt(),
                details,
                transactions.getAccountEntity() == null ? "" : transactions.getAccountEntity().getName(),
                transactions.isCash(),
                transactions.isPaid(),
                transactions.getPaidAmount(),
                transactions.getDueDate(),
                transactions.getPaymentMethodId(),
                transactions.getPaymentMethodName(),
                transactions.getPaymentMethodType(),
                transactions.getPaymentMethodRekening()
        );
    }

    private Long resolveDetailProductId(Long clientId, TransactionDetailEntity transactionDetail) {
        if (transactionDetail.getProductId() != null || transactionDetail.getDeletedAt() != null) {
            return transactionDetail.getProductId();
        }

        ProductEntity product = productRepository
                .findFirstByFullNameAndShortNameAndClientEntity_ClientIdAndDeletedAtIsNull(
                        transactionDetail.getFullName(),
                        transactionDetail.getShortName(),
                        clientId
                )
                .orElse(null);
        if (product == null) {
            return null;
        }

        transactionDetail.setProductId(product.getProductId());
        transactionDetailRepository.save(transactionDetail);
        return product.getProductId();
    }

    @Transactional
    public boolean deletePenjualan(Long transactionId, ClientEntity clientData) {
        Optional<TransactionEntity> transactionEntityOpt = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(clientData.getClientId(), transactionId);
        if (transactionEntityOpt.isEmpty()) {
            return false;
        }
        TransactionEntity transactionEntity = transactionEntityOpt.get();


        // Restore sold lines only. Pretel snapshots stay until that reversal rule is confirmed.
        List<TransactionDetailEntity> oldTransactions = transactionDetailRepository.findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(transactionId);
        for (TransactionDetailEntity old : oldTransactions) {
            ProductEntity product = productRepository.findAndLockProduct(old.getFullName(), old.getShortName(), clientData.getClientId());
            if (product != null) {
                Long restoredStock = product.getStock() + old.getQty();
                product.setStock(restoredStock);
                productRepository.save(product);

                stockMovementService.insertKartuStok(new AdjustStockDTO(
                        product,
                        transactionEntity.getTransactionNumber(),
                        TipeKartuStok.KOREKSI_PENJUALAN,
                        old.getQty(),
                        0L,
                        restoredStock,
                        clientData,
                        getCurrentTimestamp()
                ));
            }
            old.setDeletedAt(getCurrentTimestamp());
            transactionDetailRepository.save(old);
        }

        transactionEntity.setDeletedAt(getCurrentTimestamp());
        transactionRepository.save(transactionEntity);

        return true;
    }

    @Transactional
    public ResponseInBoolean payFaktur(
            Long clientId,
            Long transactionId,
            Long paymentMethodId,
            MultipartFile buktiPembayaran
    ) {
        try {
            Optional<TransactionEntity> optional = transactionRepository
                    .findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(
                            clientId,
                            transactionId
                    );
            if (optional.isEmpty()) {
                return new ResponseInBoolean(false, "Data penjualan tidak ditemukan");
            }

            TransactionEntity transaction = optional.get();
            if (transaction.isCash() || transaction.isPaid()) {
                return new ResponseInBoolean(false, "Faktur tidak bisa dilunaskan");
            }

            PaymentMethodEntity method = paymentMethodService.resolve(clientId, paymentMethodId, false);
            if (method == null) {
                return new ResponseInBoolean(false, "Metode pembayaran tidak ditemukan");
            }

            boolean transfer = "transfer".equalsIgnoreCase(method.getMethodType());
            String filePath = null;
            String originalName = null;
            if (transfer && buktiPembayaran != null && !buktiPembayaran.isEmpty()) {
                originalName = buktiPembayaran.getOriginalFilename();
                String uploadDir = "uploads/bukti/" + clientId + "/";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String fileName = System.currentTimeMillis() + "_" + originalName;
                Path path = Paths.get(uploadDir + fileName);
                Files.copy(buktiPembayaran.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                filePath = uploadDir + fileName;
            }

            paymentMethodService.copyToTransaction(transaction, method);
            transaction.setPaid(true);
            transaction.setPaidAmount(
                    transaction.getTotalPrice() == null
                            ? BigDecimal.ZERO
                            : transaction.getTotalPrice()
            );
            transactionRepository.save(transaction);

            BuktiBayarEntity bukti = new BuktiBayarEntity();
            bukti.setOriginalName(originalName);
            bukti.setFilePath(filePath);
            bukti.setTransactionEntity(transaction);
            bukti.setRekeningAsal("");
            bukti.setRekeningTujuan(method.getRekening() == null ? "" : method.getRekening());
            bukti.setJenisBayar(method.getMethodType());
            buktiBayarRepository.save(bukti);

            return new ResponseInBoolean(true, "Faktur berhasil dilunaskan");
        } catch (Exception exception) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Terjadi kesalahan: " + exception.getMessage());
        }
    }

    public BuktiBayarDTO getBuktiPembayaran(Long transactionId) {
        Optional<BuktiBayarEntity> opt = buktiBayarRepository
                .findByTransactionEntity_TransactionId(transactionId);
        if (opt.isEmpty()) {
            return new BuktiBayarDTO();
        }

        BuktiBayarEntity data = opt.get();
        BuktiBayarDTO result = new BuktiBayarDTO();
        result.setBuktiBayarId(data.getBuktiBayarId());
        result.setOriginalName(data.getOriginalName());
        result.setFilePath(data.getFilePath());
        result.setJenisBayar(data.getJenisBayar());
        result.setRekeningAsal(data.getRekeningAsal());
        result.setRekeningTujuan(data.getRekeningTujuan());
        result.setTanggalBayar(data.getCreatedAt());
        return result;
    }
}
