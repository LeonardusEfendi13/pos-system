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
import java.util.*;
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

    public Page<IndenDTO> getIndenData(String statusInden, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<IndenEntity> indenData;
        if(statusInden != null){
            indenData = indenRepository.findAllByDeletedAtIsNullAndStatusIndenAndCreatedAtBetweenOrderByCreatedAtDesc(statusInden, startDate, endDate, pageable);
        }else{
            indenData = indenRepository.findAllByDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate, pageable);
        }
        return indenData.map(this::convertToDTO);
    }

    public Page<IndenDTO> searchIndenData(String statusInden, LocalDateTime startDate, LocalDateTime endDate, String search, Pageable pageable) {
        String trimmedSearch = (search != null) ? search.trim() : "";

        if (trimmedSearch.isEmpty()) {
            return getIndenData(statusInden, startDate, endDate, pageable);
        }

        Page<IndenEntity> indenEntity = indenRepository.searchIndens(statusInden, startDate, endDate, trimmedSearch, pageable);

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
    public boolean deleteInden(Long indenId) {
        Optional<IndenEntity> indenEntityOpt = indenRepository.findFirstByIndenIdAndDeletedAtIsNull(indenId);
        if(indenEntityOpt.isEmpty()){
            return false;
        }

        IndenEntity indenEntity = indenEntityOpt.get();

        List<IndenDetailEntity> oldTransactions = indenDetailRepository.findAllByIndenEntity_IndenIdAndDeletedAtIsNullOrderByIndenDetailIdDesc(indenId);
        for (IndenDetailEntity old : oldTransactions) {
            old.setDeletedAt(getCurrentTimestamp());
            indenDetailRepository.save(old);
        }

        indenEntity.setDeletedAt(getCurrentTimestamp());
        indenRepository.save(indenEntity);

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
            System.out.println("Status Inden otw save : " + StatusInden.TERCATAT.name());
            indenEntity.setStatusInden(StatusInden.TERCATAT.name());
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

    @Transactional
    public ResponseInBoolean editTransaction(
            Long indenId,
            CreateIndenRequest req,
            AccountEntity accountData
    ) {
        String lastProduct = "-";
        ClientEntity clientData = accountData.getClientEntity();
        try {
            IndenEntity inden = indenRepository.findFirstByIndenIdAndDeletedAtIsNull(indenId).orElseThrow(() -> new RuntimeException("Transaksi tidak ditemukan"));
            inden.setSubtotal(req.getSubtotal());
            inden.setTotalPrice(req.getTotalPrice());
            inden.setTotalDiscount(req.getTotalDisc());
            inden.setAccountEntity(accountData);
            inden.setCustomerName(req.getCustomerName());
            inden.setCustomerPhone(req.getCustomerPhone());
            inden.setDeposit(req.getDeposit());
            inden.setStatusInden(StatusInden.TERCATAT.name());
            indenRepository.save(inden);

            indenDetailRepository.deleteAllByIndenEntity_IndenId(indenId);

            for (IndenDetailDTO dto : req.getIndenDetailDTOS()) {
                ProductEntity product = productRepository.findAndLockProduct(
                        dto.getName(),
                        dto.getCode(),
                        clientData.getClientId()
                );

                IndenDetailEntity detail = new IndenDetailEntity();
                detail.setShortName(dto.getCode());
                detail.setFullName(dto.getName());
                detail.setQty(dto.getQty());
                detail.setPrice(dto.getPrice());
                detail.setDiscountAmount(dto.getDiscAmount());
                detail.setTotalPrice(dto.getTotal());
                detail.setIndenEntity(inden);
                detail.setBasicPrice(product.getSupplierPrice());
                BigDecimal totalBasic = product.getSupplierPrice()
                        .multiply(BigDecimal.valueOf(dto.getQty()));
                detail.setTotalProfit(dto.getTotal().subtract(totalBasic));
                indenDetailRepository.save(detail);
            }

            return new ResponseInBoolean(true, inden.getIndenNumber());

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false,
                    e.getMessage() + " (ERROR di produk: " + lastProduct + ")");
        }
    }
}
