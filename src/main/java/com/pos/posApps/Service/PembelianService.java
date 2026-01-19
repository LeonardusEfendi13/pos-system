package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
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

@Service
public class PembelianService {
    @Autowired
    PurchasingDetailRepository purchasingDetailRepository;

    @Autowired
    PurchasingRepository purchasingRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    StockMovementService stockMovementService;

    @Autowired
    ProductPricesRepository productPricesRepository;

    @Autowired
    BuktiBayarRepository buktiBayarRepository;

    public Page<PembelianDTO> getPembelianData(
            AccountEntity accountData,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long supplierId,
            Boolean lunas,
            Boolean tunai,
            Pageable pageable) {
        Long clientId = accountData.getClientEntity().getClientId();

        Page<PurchasingEntity> purchasingData = purchasingRepository.findPurchasingData(
                clientId, supplierId, lunas, tunai, startDate, endDate, pageable
        );

        return purchasingData.map(this::convertToDTO);
    }

    public Page<PembelianDTO> searchPembelianData(AccountEntity accountData, LocalDateTime startDate, LocalDateTime endDate, Long supplierId, Boolean lunas, Boolean tunai, String search, Pageable pageable) {
        String trimmedSearch = (search != null) ? search.trim() : "";
        Long clientId = accountData.getClientEntity().getClientId();
        if (trimmedSearch.isEmpty()) {
            return getPembelianData(accountData, startDate, endDate, supplierId, lunas, tunai, pageable);
        }

        Page<PurchasingEntity> purchasingData = purchasingRepository
                .searchPurchasings(
                        clientId,
                        startDate,
                        endDate,
                        supplierId,
                        lunas,
                        tunai,
                        trimmedSearch,
                        pageable
                );

        return purchasingData.map(this::convertToDTO);
    }

    private PembelianDTO convertToDTO(PurchasingEntity purchasings) {
        String name = "Unknown";
        if(purchasings.getAccountEntity() != null) {
            if (purchasings.getAccountEntity().getName() != null && !purchasings.getAccountEntity().getName().isBlank()) {
                name = purchasings.getAccountEntity().getName();
            }
        }
        return new PembelianDTO(
                purchasings.getPurchasingId(),
                purchasings.getPurchasingNumber(),
                purchasings.getPoDate(),
                purchasings.isCash(),
                purchasings.getPoDueDate(),
                purchasings.getTotalPrice(),
                purchasings.getTotalDiscount(),
                new SupplierDTO(
                        purchasings.getSupplierEntity().getSupplierId(),
                        purchasings.getSupplierEntity().getSupplierName()
                ),
                purchasings.isPaid(),
                purchasings.getSubtotal(),
                purchasings.getPurchasingDetailEntities().stream()
                        .map(purchasingDetail -> new PembelianDetailDTO(
                                purchasingDetail.getPurchasingDetailId(),
                                purchasingDetail.getShortName(),
                                purchasingDetail.getFullName(),
                                purchasingDetail.getQty(),
                                purchasingDetail.getPrice(),
                                purchasingDetail.getDiscAmount(),
                                purchasingDetail.getTotalPrice(),
                                purchasingDetail.getMarkup1(),
                                purchasingDetail.getMarkup2(),
                                purchasingDetail.getMarkup3(),
                                purchasingDetail.getHargaJual1(),
                                purchasingDetail.getHargaJual2(),
                                purchasingDetail.getHargaJual3()
                        ))
                        .collect(Collectors.toList()),
                name
        );
    }

    public boolean checkNoFaktur(String noFaktur, ClientEntity clientData, Long supplierId) {
        Optional<PurchasingEntity> purchasingsOpt = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingNumberAndSupplierEntity_SupplierId(clientData.getClientId(), noFaktur, supplierId);
        return purchasingsOpt.isEmpty();
    }

    public PembelianDTO getPembelianDataById(Long clientId, Long pembelianId) {
        Optional<PurchasingEntity> purchasingsOpt = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, pembelianId);
        if (purchasingsOpt.isEmpty()) {
            return null;
        }
        PurchasingEntity purchasings = purchasingsOpt.get();
        return new PembelianDTO(
                purchasings.getPurchasingId(),
                purchasings.getPurchasingNumber(),
                purchasings.getPoDate(),
                purchasings.isCash(),
                purchasings.getPoDueDate(),
                purchasings.getTotalPrice(),
                purchasings.getTotalDiscount(),
                new SupplierDTO(
                        purchasings.getSupplierEntity().getSupplierId(),
                        purchasings.getSupplierEntity().getSupplierName()
                ),
                purchasings.isPaid(),
                purchasings.getSubtotal(),
                purchasings.getPurchasingDetailEntities().stream()
                        .map(purchasingDetail -> new PembelianDetailDTO(
                                purchasingDetail.getPurchasingDetailId(),
                                purchasingDetail.getShortName(),
                                purchasingDetail.getFullName(),
                                purchasingDetail.getQty(),
                                purchasingDetail.getPrice(),
                                purchasingDetail.getDiscAmount(),
                                purchasingDetail.getTotalPrice(),
                                purchasingDetail.getMarkup1(),
                                purchasingDetail.getMarkup2(),
                                purchasingDetail.getMarkup3(),
                                purchasingDetail.getHargaJual1(),
                                purchasingDetail.getHargaJual2(),
                                purchasingDetail.getHargaJual3()
                        ))
                        .collect(Collectors.toList()),
                purchasings.getAccountEntity().getName()
        );
    }

    @Transactional
    public boolean deletePurchasing(Long purchasingId, ClientEntity clientData) {
        Optional<PurchasingEntity> transactionEntityOpt = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(clientData.getClientId(), purchasingId);
        if (transactionEntityOpt.isEmpty()) {
            return false;
        }
        PurchasingEntity transactionEntity = transactionEntityOpt.get();

        //Restore stock from old purchasing detail
        List<PurchasingDetailEntity> oldTransactions = purchasingDetailRepository.findAllByPurchasingEntity_PurchasingIdOrderByPurchasingDetailIdDesc(purchasingId);
        for (PurchasingDetailEntity old : oldTransactions) {
//            ProductEntity product = productRepository.findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(old.getFullName(), old.getShortName(), clientData.getClientId());
            ProductEntity product = productRepository.findAndLockProduct(old.getFullName(), old.getShortName(), clientData.getClientId());

            if (product != null) {
                Long restoredStock = product.getStock() - old.getQty();
                product.setStock(restoredStock);
                productRepository.save(product);

                stockMovementService.insertKartuStok(new AdjustStockDTO(
                        product,
                        transactionEntity.getPurchasingNumber(),
                        TipeKartuStok.KOREKSI_PEMBELIAN,
                        0L,
                        old.getQty(),
                        restoredStock,
                        clientData
                ));
            }
            old.setDeletedAt(getCurrentTimestamp());
            purchasingDetailRepository.save(old);
        }

        transactionEntity.setDeletedAt(getCurrentTimestamp());
        purchasingRepository.save(transactionEntity);

        return true;
    }

    @Transactional
    public ResponseInBoolean createTransaction(CreatePurchasingRequest req, AccountEntity accountData) {
        String lastProduct = "Tanya Leon";
        ClientEntity clientData = accountData.getClientEntity();
        //Cek no faktur
        Optional<PurchasingEntity> pembelian = purchasingRepository.findFirstByPurchasingNumberAndClientEntity_ClientIdAndDeletedAtIsNull(req.getPurchasingNumber(), clientData.getClientId());
        if (pembelian.isPresent()) {
            return new ResponseInBoolean(false, "Nomor Faktur Sudah Ada");
        }
        try {
            //Get Supplier Entity
            Optional<SupplierEntity> supplierEntity = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientData.getClientId());
            if (supplierEntity.isEmpty()) {
                return new ResponseInBoolean(false, "Supplier Tidak ada");
            }
            //Get last Transaction id
//            Long lastPurchasingId = purchasingRepository.findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByPurchasingIdDesc(clientData.getClientId()).map(PurchasingEntity::getPurchasingId).orElse(0L);
//            Long newPurchasingId = Generator.generateId(lastPurchasingId);

            //insert the transaction data
            PurchasingEntity purchasingEntity = new PurchasingEntity();
            purchasingEntity.setPurchasingNumber(req.getPurchasingNumber());
            purchasingEntity.setSupplierEntity(supplierEntity.get());
            purchasingEntity.setTotalPrice(req.getTotalPrice());
            purchasingEntity.setTotalDiscount(req.getTotalDisc());
            purchasingEntity.setPoDate(LocalDate.parse(req.getPoDate()).atStartOfDay());
            if (!req.isCash()) {
                purchasingEntity.setPoDueDate(LocalDate.parse(req.getPoDueDate()).atStartOfDay());
            }
            purchasingEntity.setCash(req.isCash());
            //Assign isPaid with isCash
            purchasingEntity.setPaid(req.isCash());
            purchasingEntity.setClientEntity(clientData);
            purchasingEntity.setSubtotal(req.getSubtotal());
            purchasingEntity.setAccountEntity(accountData);
            purchasingRepository.save(purchasingEntity);

            //Insert all the transaction details
//            Long lastTransactionDetailId = purchasingDetailRepository.findFirstByDeletedAtIsNullOrderByPurchasingDetailIdDesc().map(PurchasingDetailEntity::getPurchasingDetailId).orElse(0L);
//            Long newPurchasingDetailId = Generator.generateId(lastTransactionDetailId);

            for (PurchasingDetailDTO dtos : req.getPembelianDetailDTOS()) {
//                ProductEntity productEntity = productRepository.findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(dtos.getName(), dtos.getCode(), clientData.getClientId());
                ProductEntity productEntity = productRepository.findAndLockProduct(dtos.getName(), dtos.getCode(), clientData.getClientId());

                PurchasingDetailEntity purchasingDetailEntity = new PurchasingDetailEntity();
                lastProduct = dtos.getCode();
//                purchasingDetailEntity.setPurchasingDetailId(newPurchasingDetailId);
                purchasingDetailEntity.setShortName(dtos.getCode());
                purchasingDetailEntity.setFullName(dtos.getName());
                purchasingDetailEntity.setQty(dtos.getQty());
                purchasingDetailEntity.setPrice(dtos.getPrice());
                purchasingDetailEntity.setDiscAmount(dtos.getDiscAmount());
                purchasingDetailEntity.setTotalPrice(dtos.getTotal());
                purchasingDetailEntity.setPurchasingEntity(purchasingEntity);

                int counter = 0;
                for (ProductPricesEntity productPriceData : productEntity.getProductPricesEntity()) {
                    counter++;
                    if (productPriceData.getPrice() != null && productPriceData.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                        if (counter == 1) {
                            purchasingDetailEntity.setMarkup1(dtos.getMarkup1());
                            purchasingDetailEntity.setHargaJual1(dtos.getHargaJual1());
                        } else if (counter == 2) {
                            purchasingDetailEntity.setMarkup2(dtos.getMarkup2());
                            purchasingDetailEntity.setHargaJual2(dtos.getHargaJual2());
                        } else if (counter == 3) {
                            purchasingDetailEntity.setMarkup3(dtos.getMarkup3());
                            purchasingDetailEntity.setHargaJual3(dtos.getHargaJual3());
                        }
                    }
                }
                purchasingDetailRepository.save(purchasingDetailEntity);
//                newPurchasingDetailId = Generator.generateId(newPurchasingDetailId);
                //Update product stock
                Long newStock = productEntity.getStock() + dtos.getQty();
                productEntity.setSupplierPrice(dtos.getPrice());
                productEntity.setStock(newStock);
                productRepository.save(productEntity);

                //Update Product Prices
                List<ProductPricesEntity> productPricesList = productPricesRepository.findAllByProductEntity_ProductIdOrderByProductPricesIdAsc(productEntity.getProductId());
                int index = 0;
                for (ProductPricesEntity data : productPricesList) {
                    if (index == 0 && dtos.getHargaJual1() != null) {
                        data.setPrice(dtos.getHargaJual1());
                        data.setPercentage(dtos.getMarkup1());
                    } else if (index == 1 && dtos.getHargaJual2() != null) {
                        data.setPrice(dtos.getHargaJual2());
                        data.setPercentage(dtos.getMarkup2());
                    } else if (index == 2 && dtos.getHargaJual3() != null) {
                        data.setPrice(dtos.getHargaJual3());
                        data.setPercentage(dtos.getMarkup3());
                    }
                    productPricesRepository.save(data);
                    index++;
                }

                //Insert kartu stok
                stockMovementService.insertKartuStok(new AdjustStockDTO(
                        productEntity,
                        req.getPurchasingNumber(),
                        TipeKartuStok.PEMBELIAN,
                        dtos.getQty(),
                        0L,
                        newStock,
                        clientData
                ));
            }
            return new ResponseInBoolean(true, "Berhasil simpan data");
        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage() + ". Error karena : " + lastProduct);
        }
    }

    @Transactional
    public ResponseInBoolean editTransaction(
            Long purchasingId,
            CreatePurchasingRequest req,
            AccountEntity accountData
    ) {
        String lastProduct = "-";
        ClientEntity clientData = accountData.getClientEntity();
        // =========================
        // DUPLICATE CHECK
        // =========================
        boolean duplicate = purchasingRepository
                .existsByPurchasingNumberAndClientEntity_ClientIdAndDeletedAtIsNullAndPurchasingIdNot(
                        req.getPurchasingNumber(), clientData.getClientId(), purchasingId
                );
        if (duplicate) {
            return new ResponseInBoolean(false, "Nomor faktur sudah ada");
        }

        try {
            // =========================
            // VALIDASI SUPPLIER
            // =========================
            SupplierEntity supplier = supplierRepository
                    .findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(
                            req.getSupplierId(), clientData.getClientId()
                    )
                    .orElseThrow(() -> new RuntimeException("Supplier tidak ada"));

            // =========================
            // VALIDASI PURCHASING
            // =========================
            PurchasingEntity purchasing = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(
                            clientData.getClientId(), purchasingId
                    )
                    .orElseThrow(() -> new RuntimeException("Data pembelian tidak ada"));

            // =========================
            // UPDATE HEADER
            // =========================
            purchasing.setPurchasingNumber(req.getPurchasingNumber());
            purchasing.setSupplierEntity(supplier);
            purchasing.setTotalPrice(req.getTotalPrice());
            purchasing.setTotalDiscount(req.getTotalDisc());
            purchasing.setSubtotal(req.getSubtotal());
            purchasing.setPoDate(LocalDate.parse(req.getPoDate()).atStartOfDay());
            purchasing.setCash(req.isCash());
            purchasing.setAccountEntity(accountData);
            if (!req.isCash()) {
                purchasing.setPoDueDate(LocalDate.parse(req.getPoDueDate()).atStartOfDay());
            }

            purchasingRepository.save(purchasing);

            // =========================
            // MAP DETAIL LAMA
            // =========================
            List<PurchasingDetailEntity> oldDetails =
                    purchasingDetailRepository.findAllByPurchasingEntity_PurchasingIdOrderByPurchasingDetailIdDesc(purchasingId);

            Map<String, PurchasingDetailEntity> oldMap = new HashMap<>();
            for (PurchasingDetailEntity d : oldDetails) {
                oldMap.put(d.getShortName(), d);
            }

            // =========================
            // MAP DETAIL BARU
            // =========================
            Map<String, PurchasingDetailDTO> newMap = new HashMap<>();
            for (PurchasingDetailDTO dto : req.getPembelianDetailDTOS()) {
                if (dto != null) {
                    newMap.put(dto.getCode(), dto);
                }
            }

            // =========================
            // UNION SEMUA PRODUK
            // =========================
            Set<String> allCodes = new HashSet<>();
            allCodes.addAll(oldMap.keySet());
            allCodes.addAll(newMap.keySet());

            // =========================
            // DELTA STOCK LOOP
            // =========================
            for (String code : allCodes) {

                PurchasingDetailEntity oldDetail = oldMap.get(code);
                PurchasingDetailDTO newDetail = newMap.get(code);

                long oldQty = oldDetail != null ? oldDetail.getQty() : 0;
                long newQty = newDetail != null ? newDetail.getQty() : 0;
                long delta = newQty - oldQty;

                // Qty sama → JANGAN SENTUH STOCK
                if (delta == 0) continue;

                lastProduct = code;

                ProductEntity product = productRepository.findAndLockProduct(
                        newDetail != null ? newDetail.getName() : oldDetail.getFullName(),
                        code,
                        clientData.getClientId()
                );

                if (product == null) {
                    throw new RuntimeException("Produk " + code + " tidak ditemukan");
                }

                // =========================
                // UPDATE STOCK (PEMBELIAN)
                // =========================
                long newStock = product.getStock() + delta;
                product.setStock(newStock);

                // Update harga beli terakhir
                if (newDetail != null) {
                    product.setSupplierPrice(newDetail.getPrice());
                }

                productRepository.save(product);

                // =========================
                // KARTU STOK
                // =========================
                stockMovementService.insertKartuStok(new AdjustStockDTO(
                        product,
                        purchasing.getPurchasingNumber(),
                        TipeKartuStok.KOREKSI_PEMBELIAN,
                        delta > 0 ? delta : 0L,            // IN
                        delta < 0 ? Math.abs(delta) : 0L, // OUT
                        newStock,
                        clientData
                ));
            }

            // =========================
            // REPLACE DETAIL
            // =========================
            purchasingDetailRepository.deleteAllByPurchasingEntity_PurchasingId(purchasingId);

            for (PurchasingDetailDTO dto : req.getPembelianDetailDTOS()) {
                if (dto == null) continue;

                ProductEntity product = productRepository.findAndLockProduct(
                        dto.getName(),
                        dto.getCode(),
                        clientData.getClientId()
                );

                PurchasingDetailEntity detail = new PurchasingDetailEntity();
                detail.setShortName(dto.getCode());
                detail.setFullName(dto.getName());
                detail.setQty(dto.getQty());
                detail.setPrice(dto.getPrice());
                detail.setDiscAmount(dto.getDiscAmount());
                detail.setTotalPrice(dto.getTotal());
                detail.setPurchasingEntity(purchasing);

                detail.setMarkup1(dto.getMarkup1());
                detail.setHargaJual1(dto.getHargaJual1());
                detail.setMarkup2(dto.getMarkup2());
                detail.setHargaJual2(dto.getHargaJual2());
                detail.setMarkup3(dto.getMarkup3());
                detail.setHargaJual3(dto.getHargaJual3());

                purchasingDetailRepository.save(detail);

                // Update product prices
                List<ProductPricesEntity> prices =
                        productPricesRepository.findAllByProductEntity_ProductIdOrderByProductPricesIdAsc(
                                product.getProductId()
                        );

                if (prices.size() > 0 && dto.getHargaJual1() != null) {
                    prices.get(0).setPrice(dto.getHargaJual1());
                    prices.get(0).setPercentage(dto.getMarkup1());
                }
                if (prices.size() > 1 && dto.getHargaJual2() != null) {
                    prices.get(1).setPrice(dto.getHargaJual2());
                    prices.get(1).setPercentage(dto.getMarkup2());
                }
                if (prices.size() > 2 && dto.getHargaJual3() != null) {
                    prices.get(2).setPrice(dto.getHargaJual3());
                    prices.get(2).setPercentage(dto.getMarkup3());
                }

                productPricesRepository.saveAll(prices);
            }

            return new ResponseInBoolean(true, "Data berhasil disimpan");

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false,
                    e.getMessage() + " (ERROR di produk: " + lastProduct + ")");
        }
    }


    @Transactional
    public ResponseInBoolean payFaktur(Long clientId, LunaskanPembelianDTO req) {
        try {
            // Find target purchasing entity
            Optional<PurchasingEntity> optional = purchasingRepository
                    .findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(
                            clientId, req.getPembelianId());

            if (optional.isEmpty()) {
                return new ResponseInBoolean(false, "Data pembelian tidak ditemukan");
            }

            PurchasingEntity purchasingEntity = optional.get();

            // ===== Handle File Upload =====
            String filePath = null;
            String originalName = null;

            if ("transfer".equalsIgnoreCase(req.getJenisPembayaran()) && req.getBuktiPembayaran() != null && !req.getBuktiPembayaran().isEmpty()) {
                MultipartFile file = req.getBuktiPembayaran();
                originalName = file.getOriginalFilename();

                // Folder path (can use clientId for separation)
                String uploadDir = "uploads/bukti/" + clientId + "/";
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                // Unique file name
                String fileName = System.currentTimeMillis() + "_" + originalName;
                Path path = Paths.get(uploadDir + fileName);

                // Save file locally
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                filePath = uploadDir + fileName;
            }

            // ===== Update purchasing & save bukti =====
            purchasingEntity.setPaid(true);
            purchasingRepository.save(purchasingEntity);

            // Save Bukti Bayar record if transfer
            BuktiBayarEntity bukti = new BuktiBayarEntity();
            bukti.setOriginalName(originalName);
            bukti.setFilePath(filePath);
            bukti.setPurchasingEntity(purchasingEntity);
            bukti.setRekeningAsal(req.getRekeningAsal());
            bukti.setRekeningTujuan(req.getRekeningTujuan());
            bukti.setJenisBayar(req.getJenisPembayaran());
            buktiBayarRepository.save(bukti);

            return new ResponseInBoolean(true, "Faktur berhasil dilunaskan");

        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Terjadi kesalahan: " + e.getMessage());
        }
    }

    public BuktiBayarDTO getBuktiPembayaran(Long pembelianId) {
        Optional<BuktiBayarEntity> opt = buktiBayarRepository.findByPurchasingEntity_PurchasingId(pembelianId);
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
