package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.StatusInden;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    @Autowired
    KasirService kasirService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    IndenDetailRepository indenDetailRepository;

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
                indens.getDeposit(),
                indens.getTotalPrice().subtract(indens.getDeposit()),
                name,
                indens.getCustomerName(),
                indens.getCustomerPhone(),
                indens.getStatusInden()
        );
    }

    public IndenDTO getPenjualanDataById(Long penjualanId) {
        Optional<IndenEntity> indenOpt = indenRepository.findFirstByIndenIdAndDeletedAtIsNull(penjualanId);
        if (indenOpt.isEmpty()) {
            return null;
        }
        IndenEntity indenEntity = indenOpt.get();
        return convertToDTO(indenEntity);
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

    @Transactional
    public ResponseInBoolean createTransaction(CreateIndenRequest req, AccountEntity accountData) {
        String lastProduct = "Tanya Leon";
        ClientEntity clientData = accountData.getClientEntity();
        try {
            String generatedNotaNumber = kasirService.generateTodayNota(clientData.getClientId());

            //insert the Inden data
            IndenEntity indenEntity = new IndenEntity();
            indenEntity.setIndenNumber(generatedNotaNumber);
            indenEntity.setSubtotal(req.getSubtotal());
            indenEntity.setTotalPrice(req.getTotalPrice());
            indenEntity.setTotalDiscount(req.getTotalDisc());
            indenEntity.setAccountEntity(accountData);
            indenEntity.setCustomerName(req.getCustomerName());
            indenEntity.setCustomerPhone(req.getCustomerPhone());
            indenEntity.setDeposit(req.getDeposit());
            indenEntity.setStatusInden(StatusInden.TERCATAT);
            indenRepository.save(indenEntity);

            System.out.println("=====START LOG ID : " + indenEntity.getIndenId() + "=======");

            for(IndenDetailDTO  dtos: req.getIndenDetailDTOS()){
                System.out.println("Part Number : " + dtos.getCode());
                System.out.println("Nama Barang : " + dtos.getName());

                //Get Product Entity
                ProductEntity productEntity = productRepository.findAndLockProduct(dtos.getName(), dtos.getCode(), clientData.getClientId());
                entityManager.refresh(productEntity);

                if(productEntity == null){
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return new ResponseInBoolean(true, "Produk " + dtos.getName() + " tidak ditemukan");
                }

                System.out.println("Produk: " + productEntity.getShortName() + "(" +productEntity.getStock() + ") VALID");
                lastProduct = dtos.getCode();

                IndenDetailEntity indenDetailEntity = new IndenDetailEntity();
                indenDetailEntity.setShortName(dtos.getCode());
                indenDetailEntity.setFullName(dtos.getName());
                indenDetailEntity.setQty(dtos.getQty());
                indenDetailEntity.setPrice(dtos.getPrice());
                indenDetailEntity.setDiscountAmount(dtos.getDiscAmount());
                indenDetailEntity.setTotalPrice(dtos.getTotal());
                indenDetailEntity.setIndenEntity(indenEntity);
                indenDetailEntity.setBasicPrice(productEntity.getSupplierPrice());
                BigDecimal totalBasicPrice = productEntity.getSupplierPrice().multiply(BigDecimal.valueOf(dtos.getQty()));
                BigDecimal totalProfit = dtos.getTotal().subtract(totalBasicPrice);
                indenDetailEntity.setTotalProfit(totalProfit);
                indenDetailRepository.save(indenDetailEntity);
            }
            System.out.println("=====END LOG=======");
            System.out.println();
            return new ResponseInBoolean(true, generatedNotaNumber);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage() + ". ERROR KARENA : " + lastProduct);
        }
    }
}
