package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.IndenRepository;
import com.pos.posApps.Repository.ProductRepository;
import com.pos.posApps.Repository.TransactionDetailRepository;
import com.pos.posApps.Repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class IndenService {
    @Autowired
    TransactionDetailRepository transactionDetailRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    StockMovementService stockMovementService;

    @Autowired
    IndenRepository indenRepository;

    public BigDecimal getTotalRevenues(Long clientId) {
        LocalDateTime startDate = LocalDate.now().atStartOfDay();
        LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);
        List<TransactionEntity> transactionData = transactionRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate).stream().toList();
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
        List<TransactionEntity> transactionData = transactionRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate).stream().limit(20).toList();
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
                transactions.getAccountEntity().getName()

        )).collect(Collectors.toList());
    }

    // File: PenjualanService.java

    public Page<IndenDTO> getIndenData(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<IndenEntity> indenData;
        indenData = indenRepository.findAllByDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate, pageable);
        return indenData.map(this::convertToDTO);
    }

    public Page<IndenDTO> searchIndenData(LocalDateTime startDate, LocalDateTime endDate, String search, Pageable pageable) {
        String trimmedSearch = (search != null) ? search.trim() : "";

        if (trimmedSearch.isEmpty()) {
            return getIndenData(startDate, endDate, pageable);
        }

        Page<IndenEntity> indenEntity = indenRepository.searchIndens(startDate, endDate, trimmedSearch, pageable);

        return indenEntity.map(this::convertToDTO);
    }

    private IndenDTO convertToDTO(IndenEntity indens) {
        String name = "Unknown";
        if(indens.getAccountEntity() != null) {
            if (indens.getAccountEntity().getName() != null && !indens.getAccountEntity().getName().isBlank()) {
                name = indens.getAccountEntity().getName();
            }
        }
        return new IndenDTO(
                indens.getIndenId(),
                indens.getIndenNumber(),
                indens.getSubtotal(),
                indens.getTotalPrice(),
                indens.getTotalDiscount(),
                indens.getCreatedAt(),
                indens.getIndenDetailEntities().stream().map(indenDetail -> new IndenDetailDTO(
                        indenDetail.getShortName(),
                        indenDetail.getFullName(),
                        indenDetail.getPrice(),
                        indenDetail.getQty(),
                        indenDetail.getDiscountAmount(),
                        indenDetail.getTotalPrice(),
                        indenDetail.getTotalProfit(),
                        indenDetail.getBasicPrice()
                )).collect(Collectors.toList()),
                name,
                indens.getCustomerName(),
                indens.getCustomerPhone(),
                indens.getIsOrdered(),
                indens.getIsAvailable(),
                indens.getIsDelivered()
        );
    }

    public PenjualanDTO getPenjualanDataById(Long clientId, Long penjualanId) {
        Optional<TransactionEntity> transactionsOpt = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(clientId, penjualanId);
        if (transactionsOpt.isEmpty()) {
            return null;
        }
        TransactionEntity transactions = transactionsOpt.get();
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
                transactions.getAccountEntity().getName()
        );
    }

    @Transactional
    public boolean deletePenjualan(Long transactionId, ClientEntity clientData) {
        Optional<TransactionEntity> transactionEntityOpt = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndDeletedAtIsNull(clientData.getClientId(), transactionId);
        if (transactionEntityOpt.isEmpty()) {
            return false;
        }
        TransactionEntity transactionEntity = transactionEntityOpt.get();


        //Restore stock from old transaction
        List<TransactionDetailEntity> oldTransactions = transactionDetailRepository.findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(transactionId);
        for (TransactionDetailEntity old : oldTransactions) {
//            ProductEntity product = productRepository.findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(old.getFullName(), old.getShortName(), clientData.getClientId());
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
                        clientData
                ));
            }
            old.setDeletedAt(getCurrentTimestamp());
            transactionDetailRepository.save(old);
        }

        transactionEntity.setDeletedAt(getCurrentTimestamp());
        transactionRepository.save(transactionEntity);

        return true;
    }
}
