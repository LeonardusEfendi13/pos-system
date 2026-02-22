package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.PreorderDetailRepository;
import com.pos.posApps.Repository.PreorderRepository;
import com.pos.posApps.Repository.ProductRepository;
import com.pos.posApps.Repository.SupplierRepository;
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
public class PreorderService {
    @Autowired
    PreorderRepository preorderRepository;

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    PreorderDetailRepository preorderDetailRepository;

    @Autowired
    ProductRepository productRepository;

    public Page<PreorderEntity> getPreorderData(Long clientId, Long supplierId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        if (supplierId == null) {
            return preorderRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByPreorderIdDesc(clientId, startDate, endDate, pageable);
        } else {
            return preorderRepository.findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByPreorderIdDesc(clientId, supplierId, startDate, endDate, pageable);
        }
    }

    public Page<PreorderEntity> searchPreorderData(Long clientId, Long supplierId, LocalDateTime startDate, LocalDateTime endDate, String search, Pageable pageable) {
        String trimmedSearch = (search != null) ? search.trim() : "";

        if (trimmedSearch.isEmpty()) {
            return getPreorderData(clientId, supplierId, startDate, endDate, pageable);
        }

        return preorderRepository.searchPreorders(
                clientId,
                supplierId,
                startDate,
                endDate,
                trimmedSearch,
                pageable
        );
    }

    public PreorderDTO getPreorderDataById(Long clientId, Long preorderId) {
        Optional<PreorderEntity> preorderOpt = preorderRepository.findFirstByClientEntity_ClientIdAndPreorderIdAndPreorderDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, preorderId);
        if (preorderOpt.isEmpty()) {
            return null;
        }
        PreorderEntity preorders = preorderOpt.get();
        return new PreorderDTO(
                preorders.getPreorderId(),
                new SupplierDTO(
                        preorders.getSupplierEntity().getSupplierId(),
                        preorders.getSupplierEntity().getSupplierName()
                ),
                preorders.getSubtotal(),
                preorders.getTotalPrice(),
                preorders.getTotalDiscount(),
                preorders.getCreatedAt(),
                preorders.getPreorderDetailEntities().stream()
                        .map(preorderDetail -> new PreorderDetailDTO(
                                preorderDetail.getPreorderDetailId(),
                                preorderDetail.getShortName(),
                                preorderDetail.getFullName(),
                                preorderDetail.getQuantity(),
                                preorderDetail.getPrice(),
                                preorderDetail.getDiscountAmount(),
                                preorderDetail.getTotalPrice()
                        ))
                        .collect(Collectors.toList())
        );
    }

    @Transactional
    public ResponseInBoolean createTransaction(CreatePreorderRequest req, ClientEntity clientData) {
        try {
            //Get Supplier Entity
            Optional<SupplierEntity> supplierEntityOpt = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientData.getClientId());
            if (supplierEntityOpt.isEmpty()) {
                return new ResponseInBoolean(true, "Supplier tidak ada");
            }
            SupplierEntity supplierEntity = supplierEntityOpt.get();

            if (req.getPreorderDetailDTOS() == null) {
                return new ResponseInBoolean(true, "Data tidak diterima");
            }

            PreorderEntity preorderEntity = new PreorderEntity();
            preorderEntity.setSupplierEntity(supplierEntity);
            preorderEntity.setClientEntity(clientData);
            preorderEntity.setSubtotal(req.getSubtotal());
            preorderEntity.setTotalPrice(req.getTotalPrice());
            preorderEntity.setTotalDiscount(req.getTotalDisc());
            preorderRepository.save(preorderEntity);

            // ✅ Sort preorder details alphabetically by name (A–Z)
            if (req.getPreorderDetailDTOS() != null && !req.getPreorderDetailDTOS().isEmpty()) {
                req.getPreorderDetailDTOS().sort(Comparator.comparing(PreorderDetailDTO::getName, String.CASE_INSENSITIVE_ORDER));
            }

            for (PreorderDetailDTO dtos : req.getPreorderDetailDTOS()) {
                PreorderDetailEntity preorderDetailEntity = new PreorderDetailEntity();
                preorderDetailEntity.setShortName(dtos.getCode());
                preorderDetailEntity.setFullName(dtos.getName());
                preorderDetailEntity.setQuantity(dtos.getQty());
                preorderDetailEntity.setPrice(dtos.getPrice());
                preorderDetailEntity.setDiscountAmount(dtos.getDiscAmount());
                preorderDetailEntity.setTotalPrice(dtos.getTotal());
                preorderDetailEntity.setPreorderEntity(preorderEntity);
                preorderDetailRepository.save(preorderDetailEntity);
            }
            return new ResponseInBoolean(true, "Preorder berhasil dibuat");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage());
        }
    }

    @Transactional
    public ResponseInBoolean editTransaction(Long preorderId, CreatePreorderRequest req, Long clientId) {
        try {
            //Get Supplier Entity
            Optional<SupplierEntity> supplierEntityOpt = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientId);
            if (supplierEntityOpt.isEmpty()) {
                return new ResponseInBoolean(true, "Supplier tidak ada");
            }
            SupplierEntity supplierEntity = supplierEntityOpt.get();

            //Check if transaction exist
            Optional<PreorderEntity> preorderEntityOpt = preorderRepository.findFirstByClientEntity_ClientIdAndPreorderIdAndPreorderDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, preorderId);

            if (preorderEntityOpt.isEmpty()) {
                return new ResponseInBoolean(false, "Data transaksi tidak ditemukan");
            }
            PreorderEntity preorderEntity = preorderEntityOpt.get();

            //insert the transaction data
            preorderEntity.setSupplierEntity(supplierEntity);
            preorderEntity.setTotalPrice(req.getTotalPrice());
            preorderEntity.setTotalDiscount(req.getTotalDisc());
            preorderEntity.setSubtotal(req.getSubtotal());
            preorderRepository.save(preorderEntity);

            //Delete all product prices related to product id
            preorderDetailRepository.deleteAllByPreorderEntity_PreorderId(preorderId);

            // ✅ Sort preorder details alphabetically by name (A–Z)
            if (req.getPreorderDetailDTOS() != null && !req.getPreorderDetailDTOS().isEmpty()) {
                req.getPreorderDetailDTOS().sort(Comparator.comparing(PreorderDetailDTO::getName, String.CASE_INSENSITIVE_ORDER));
            }

            for (PreorderDetailDTO dtos : req.getPreorderDetailDTOS()) {
                if (dtos != null) {
                    PreorderDetailEntity preorderDetailEntity = new PreorderDetailEntity();
                    preorderDetailEntity.setShortName(dtos.getCode());
                    preorderDetailEntity.setFullName(dtos.getName());
                    preorderDetailEntity.setQuantity(dtos.getQty());
                    preorderDetailEntity.setPrice(dtos.getPrice());
                    preorderDetailEntity.setDiscountAmount(dtos.getDiscAmount());
                    preorderDetailEntity.setTotalPrice(dtos.getTotal());
                    preorderDetailEntity.setPreorderEntity(preorderEntity);
                    preorderDetailRepository.save(preorderDetailEntity);
                }
            }
            return new ResponseInBoolean(true, "Data berhasil disimpan");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage());
        }
    }

    @Transactional
    public boolean deletePreorder(Long purchasingId, ClientEntity clientData) {
        Optional<PreorderEntity> preorderEntityOpt = preorderRepository.findFirstByClientEntity_ClientIdAndPreorderIdAndDeletedAtIsNull(clientData.getClientId(), purchasingId);
        if (preorderEntityOpt.isEmpty()) {
            return false;
        }
        PreorderEntity preorderEntity = preorderEntityOpt.get();

        preorderEntity.setDeletedAt(getCurrentTimestamp());
        preorderRepository.save(preorderEntity);

        return true;
    }

    public PembelianDTO prepareDataForKasirPembelian(Long clientId, PreorderDTO preorderDTO) {
        String name = "Unknown";
        List<String> codes = preorderDTO.getPreorderDetailDTOS()
                .stream()
                .map(PreorderDetailDTO::getCode)
                .toList();

        List<ProductEntity> products = productRepository.findAllByShortNameInAndClientEntity_ClientIdAndDeletedAtIsNull(codes, clientId);

        Map<String, ProductEntity> productMap = products.stream().collect(Collectors.toMap(ProductEntity::getShortName,p -> p));
        List<PembelianDetailDTO> pembelianDetails = new ArrayList<>();

        for (PreorderDetailDTO detail : preorderDTO.getPreorderDetailDTOS()) {

            ProductEntity productData = productMap.get(detail.getCode());

            if (productData == null) {
                throw new RuntimeException("Product tidak ditemukan: " + detail.getCode());
            }

            pembelianDetails.add(new PembelianDetailDTO(
                    null,
                    productData.getShortName(),
                    productData.getFullName(),
                    detail.getQty(),
                    detail.getPrice(),
                    detail.getDiscAmount(),
                    BigDecimal.ZERO,
                    productData.getProductPricesEntity().get(0).getPercentage(),
                    productData.getProductPricesEntity().get(1).getPercentage(),
                    productData.getProductPricesEntity().get(2).getPercentage(),
                    productData.getProductPricesEntity().get(0).getPrice(),
                    productData.getProductPricesEntity().get(1).getPrice(),
                    productData.getProductPricesEntity().get(2).getPrice()
            ));
        }
        return new PembelianDTO(
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                new SupplierDTO(
                        preorderDTO.getSupplierDTO().getSupplierId(),
                        preorderDTO.getSupplierDTO().getSupplierName()
                ),
                false,
                null,
                pembelianDetails,
                name
        );
    }


    public Boolean updatePreorderByConvertedData(){
        return true;
    }
}
