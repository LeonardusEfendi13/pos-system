package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import com.pos.posApps.Util.Generator;
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
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long supplierId,
            Boolean lunas,
            Boolean tunai,
            Pageable pageable) {

        Page<PurchasingEntity> purchasingData = purchasingRepository.findPurchasingData(
                clientId, supplierId, lunas, tunai, startDate, endDate, pageable
        );

        return purchasingData.map(this::convertToDTO);
    }

    public Page<PembelianDTO> searchPembelianData(Long clientId, LocalDateTime startDate, LocalDateTime endDate, Long supplierId, Boolean lunas, Boolean tunai, String search, Pageable pageable) {
        String trimmedSearch = (search != null) ? search.trim() : "";

        if (trimmedSearch.isEmpty()) {
            return getPembelianData(clientId, startDate, endDate, supplierId, lunas, tunai, pageable);
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
                        .collect(Collectors.toList())
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
                        .collect(Collectors.toList())
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
            ProductEntity product = productRepository.findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(old.getFullName(), old.getShortName(), clientData.getClientId());
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

//                boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
//                        product,
//                        transactionEntity.getPurchasingNumber(),
//                        TipeKartuStok.KOREKSI_PEMBELIAN,
//                        0L,
//                        old.getQty(),
//                        restoredStock,
//                        clientData
//                ));
//                if (!isAdjusted) {
//                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                    return false;
//                }
            }
            old.setDeletedAt(getCurrentTimestamp());
            purchasingDetailRepository.save(old);
        }

        transactionEntity.setDeletedAt(getCurrentTimestamp());
        purchasingRepository.save(transactionEntity);

        return true;
    }

    @Transactional
    public ResponseInBoolean createTransaction(CreatePurchasingRequest req, ClientEntity clientData) {
        String lastProduct = "Tanya Leon";
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
            Long lastPurchasingId = purchasingRepository.findFirstByClientEntity_ClientIdAndDeletedAtIsNullOrderByPurchasingIdDesc(clientData.getClientId()).map(PurchasingEntity::getPurchasingId).orElse(0L);
            Long newPurchasingId = Generator.generateId(lastPurchasingId);

            //insert the transaction data
            PurchasingEntity purchasingEntity = new PurchasingEntity();
            purchasingEntity.setPurchasingId(newPurchasingId);
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
            purchasingRepository.save(purchasingEntity);

            //Insert all the transaction details
            Long lastTransactionDetailId = purchasingDetailRepository.findFirstByDeletedAtIsNullOrderByPurchasingDetailIdDesc().map(PurchasingDetailEntity::getPurchasingDetailId).orElse(0L);
            Long newPurchasingDetailId = Generator.generateId(lastTransactionDetailId);

            for (PurchasingDetailDTO dtos : req.getPembelianDetailDTOS()) {
                ProductEntity productEntity = productRepository.findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(dtos.getName(), dtos.getCode(), clientData.getClientId());

                PurchasingDetailEntity purchasingDetailEntity = new PurchasingDetailEntity();
                lastProduct = dtos.getCode();
                purchasingDetailEntity.setPurchasingDetailId(newPurchasingDetailId);
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
                newPurchasingDetailId = Generator.generateId(newPurchasingDetailId);

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
//                boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
//                        productEntity,
//                        req.getPurchasingNumber(),
//                        TipeKartuStok.PEMBELIAN,
//                        dtos.getQty(),
//                        0L,
//                        newStock,
//                        clientData
//                ));
//                if (!isAdjusted) {
//                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                    return new ResponseInBoolean(false, "Gagal adjust di create pembelian. Errro karena  : " + lastProduct);
//                }
            }
            return new ResponseInBoolean(true, "Berhasil simpan data");
        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage() + ". Error karena : " + lastProduct);
        }
    }

    @Transactional
    public ResponseInBoolean editTransaction(Long purchasingId, CreatePurchasingRequest req, ClientEntity clientData) {
        String lastProduct ="Tanya Leon";
        // Check duplication
        boolean pembelianDuplicate = purchasingRepository.existsByPurchasingNumberAndClientEntity_ClientIdAndDeletedAtIsNullAndPurchasingIdNot(
                req.getPurchasingNumber(), clientData.getClientId(), purchasingId
        );
        if (pembelianDuplicate) {
            return new ResponseInBoolean(false, "Nomor faktur sudah ada");
        }

        try {
            // Validate supplier
            Optional<SupplierEntity> supplierEntityOpt = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(
                    req.getSupplierId(), clientData.getClientId()
            );
            if (supplierEntityOpt.isEmpty()) {
                return new ResponseInBoolean(false, "Supplier tidak ada");
            }
            SupplierEntity supplierEntity = supplierEntityOpt.get();

            // Validate purchasing
            Optional<PurchasingEntity> purchasingEntityOpt = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(
                    clientData.getClientId(), purchasingId
            );
            if (purchasingEntityOpt.isEmpty()) {
                return new ResponseInBoolean(false, "Data Pembelian tidak ada");
            }
            PurchasingEntity purchasingEntity = purchasingEntityOpt.get();

            // Update purchasing summary
            purchasingEntity.setPurchasingNumber(req.getPurchasingNumber());
            purchasingEntity.setSupplierEntity(supplierEntity);
            purchasingEntity.setTotalPrice(req.getTotalPrice());
            purchasingEntity.setTotalDiscount(req.getTotalDisc());
            purchasingEntity.setPoDate(LocalDate.parse(req.getPoDate()).atStartOfDay());
            if (!req.isCash()) {
                purchasingEntity.setPoDueDate(LocalDate.parse(req.getPoDueDate()).atStartOfDay());
            }
            purchasingEntity.setCash(req.isCash());
            purchasingEntity.setSubtotal(req.getSubtotal());
            purchasingRepository.save(purchasingEntity);

            // Build map of new purchasing detail qty for comparison
            Map<String, Long> newQtyMap = new HashMap<>();
            for (PurchasingDetailDTO dto : req.getPembelianDetailDTOS()) {
                if (dto != null) {
                    newQtyMap.put(dto.getCode(), dto.getQty());
                }
            }

            // Restore stock from old transaction (if qty changed)
            List<PurchasingDetailEntity> oldTransactions = purchasingDetailRepository.findAllByPurchasingEntity_PurchasingIdOrderByPurchasingDetailIdDesc(purchasingId);
            Map<String, PurchasingDetailEntity> oldProductMap = new HashMap<>();

            for (PurchasingDetailEntity old : oldTransactions) {
                ProductEntity product = productRepository.findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(
                        old.getFullName(), old.getShortName(), clientData.getClientId()
                );
                if (product != null) {
                    lastProduct = old.getShortName();
                    String key = old.getShortName();
                    Long newQty = newQtyMap.getOrDefault(key, null);
                    boolean isQtyChanged = (newQty == null || !Objects.equals(newQty, old.getQty()));

                    if (!isQtyChanged) {
                        oldProductMap.put(key, old); // Save for reuse
                        continue;
                    }

                    // Restore stock
                    product.setStock(product.getStock() - old.getQty()); // Undo purchase
                    productRepository.save(product);

                    // Insert kartu stok (restore)
                    stockMovementService.insertKartuStok(new AdjustStockDTO(
                            product,
                            purchasingEntity.getPurchasingNumber(),
                            TipeKartuStok.KOREKSI_PEMBELIAN,
                            0L,
                            old.getQty(),
                            product.getStock(),
                            clientData
                    ));
//                    boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
//                            product,
//                            purchasingEntity.getPurchasingNumber(),
//                            TipeKartuStok.KOREKSI_PEMBELIAN,
//                            0L,
//                            old.getQty(),
//                            product.getStock(),
//                            clientData
//                    ));
//                    if (!isAdjusted) {
//                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                        return new ResponseInBoolean(false, "Gagal adjust di edit pembelian (restore). Karena barang : " + lastProduct);
//                    }

                    oldProductMap.put(key, old); // Save for later comparison
                }
            }

            // Delete old purchasing details
            purchasingDetailRepository.deleteAllByPurchasingEntity_PurchasingId(purchasingId);

            // Insert new purchasing details
            Long lastDetailId = purchasingDetailRepository.findFirstByDeletedAtIsNullOrderByPurchasingDetailIdDesc()
                    .map(PurchasingDetailEntity::getPurchasingDetailId).orElse(0L);
            Long newDetailId = Generator.generateId(lastDetailId);

            for (PurchasingDetailDTO dto : req.getPembelianDetailDTOS()) {
                if (dto == null) continue;

                lastProduct = dto.getCode();

                ProductEntity product = productRepository.findFirstByFullNameAndShortNameAndDeletedAtIsNullAndClientEntity_ClientId(
                        dto.getName(), dto.getCode(), clientData.getClientId()
                );
                if (product == null) continue;

                // Insert detail
                PurchasingDetailEntity detail = new PurchasingDetailEntity();
                detail.setPurchasingDetailId(newDetailId);
                detail.setShortName(dto.getCode());
                detail.setFullName(dto.getName());
                detail.setQty(dto.getQty());
                detail.setPrice(dto.getPrice());
                detail.setDiscAmount(dto.getDiscAmount());
                detail.setTotalPrice(dto.getTotal());
                detail.setPurchasingEntity(purchasingEntity);

                // Set harga jual/markup
                int counter = 0;
                for (ProductPricesEntity price : product.getProductPricesEntity()) {
                    if (price.getPrice() != null && price.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                        counter++;
                        if (counter == 1) {
                            detail.setMarkup1(dto.getMarkup1());
                            detail.setHargaJual1(dto.getHargaJual1());
                        } else if (counter == 2) {
                            detail.setMarkup2(dto.getMarkup2());
                            detail.setHargaJual2(dto.getHargaJual2());
                        } else if (counter == 3) {
                            detail.setMarkup3(dto.getMarkup3());
                            detail.setHargaJual3(dto.getHargaJual3());
                        }
                    }
                }
                purchasingDetailRepository.save(detail);
                newDetailId = Generator.generateId(newDetailId);

                // Update product stock
                Long updatedStock = product.getStock() + dto.getQty();
                product.setSupplierPrice(dto.getPrice());
                product.setStock(updatedStock);
                productRepository.save(product);

                //Update Product Prices
                List<ProductPricesEntity> productPricesList = productPricesRepository.findAllByProductEntity_ProductIdOrderByProductPricesIdAsc(product.getProductId());
                int index = 0;
                for (ProductPricesEntity data : productPricesList) {
                    if (index == 0 && dto.getHargaJual1() != null) {
                        data.setPrice(dto.getHargaJual1());
                        data.setPercentage(dto.getMarkup1());
                    } else if (index == 1 && dto.getHargaJual2() != null) {
                        data.setPrice(dto.getHargaJual2());
                        data.setPercentage(dto.getMarkup2());
                    } else if (index == 2 && dto.getHargaJual3() != null) {
                        data.setPrice(dto.getHargaJual3());
                        data.setPercentage(dto.getMarkup3());
                    }
                    productPricesRepository.save(data);
                    index++;
                }

                // Check if qty has changed
                PurchasingDetailEntity oldDetail = oldProductMap.get(dto.getCode());
                if (oldDetail != null && Objects.equals(oldDetail.getQty(), dto.getQty())) {
                    continue; // Skip stock adjustment if qty is same
                }

                // Insert kartu stok (new)
                stockMovementService.insertKartuStok(new AdjustStockDTO(
                        product,
                        purchasingEntity.getPurchasingNumber(),
                        TipeKartuStok.KOREKSI_PEMBELIAN,
                        dto.getQty(),
                        0L,
                        updatedStock,
                        clientData
                ));
//                boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
//                        product,
//                        purchasingEntity.getPurchasingNumber(),
//                        TipeKartuStok.KOREKSI_PEMBELIAN,
//                        dto.getQty(),
//                        0L,
//                        updatedStock,
//                        clientData
//                ));
//                if (!isAdjusted) {
//                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                    return new ResponseInBoolean(false, "Gagal adjust stok saat insert detail pembelian");
//                }
            }

            return new ResponseInBoolean(true, "Data berhasil disimpan");

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage() + ". Errror di : "+ lastProduct);
        }
    }

    @Transactional
    public ResponseInBoolean payFaktur(Long clientId, LunaskanPembelianDTO req) {
        try {
            // Validation for transfer payments
            if ("transfer".equalsIgnoreCase(req.getJenisPembayaran())) {
                if (req.getBuktiPembayaran() == null || req.getBuktiPembayaran().isEmpty()) {
                    return new ResponseInBoolean(false, "Harap unggah bukti pembayaran");
                }
            }

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
