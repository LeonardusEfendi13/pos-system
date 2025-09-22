package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<PembelianDTO> getPembelianData(Long clientId, LocalDateTime startDate, LocalDateTime endDate, Long supplierId, Boolean lunas, Boolean tunai) {
        List<PurchasingEntity> purchasingData;
        if(supplierId == null){
            purchasingData = purchasingRepository.findAllByClientEntity_ClientIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(clientId, startDate, endDate);
        }else{
            purchasingData = purchasingRepository.findAllByClientEntity_ClientIdAndSupplierEntity_SupplierIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(clientId, supplierId, startDate, endDate);
        }
        if (tunai != null) {
            purchasingData = purchasingData.stream()
                    .filter(p -> p.isCash() == tunai)
                    .collect(Collectors.toList());
        }

        if (lunas != null && (tunai == null || !tunai)) {
            purchasingData = purchasingData.stream()
                    .filter(p -> !p.isCash() && p.isPaid() == lunas)
                    .collect(Collectors.toList());
        }
        return purchasingData.stream().map(purchasings -> new PembelianDTO(
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
                        .collect(Collectors.toList())  // collect the stream to a list
        )).collect(Collectors.toList());
    }

    public boolean checkNoFaktur(String noFaktur, ClientEntity clientData, Long supplierId){
        Optional<PurchasingEntity> purchasingsOpt = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingNumberAndSupplierEntity_SupplierId(clientData.getClientId(), noFaktur, supplierId);
        return purchasingsOpt.isEmpty();
    }

    public PembelianDTO getPembelianDataById(Long clientId, Long pembelianId) {
        Optional<PurchasingEntity> purchasingsOpt = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, pembelianId);
        if(purchasingsOpt.isEmpty()){
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
    public boolean deletePurchasing(Long purchasingId, ClientEntity clientData){
        Optional<PurchasingEntity> transactionEntityOpt = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(clientData.getClientId(), purchasingId);
        if(transactionEntityOpt.isEmpty()){
            System.out.println("Transaction not found");
            return false;
        }
        PurchasingEntity transactionEntity = transactionEntityOpt.get();

        //Restore stock from old purchasing detail
        List<PurchasingDetailEntity> oldTransactions = purchasingDetailRepository.findAllByPurchasingEntity_PurchasingIdOrderByPurchasingDetailIdDesc(purchasingId);
        for(PurchasingDetailEntity old : oldTransactions){
            ProductEntity product = productRepository.findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(old.getFullName(), old.getShortName(), clientData.getClientId());
            if(product != null){
                Long restoredStock = product.getStock() - old.getQty();
                product.setStock(restoredStock);
                productRepository.save(product);

                boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                        product,
                        transactionEntity.getPurchasingNumber(),
                        TipeKartuStok.KOREKSI_PEMBELIAN,
                        0L,
                        old.getQty(),
                        restoredStock,
                        clientData
                ));
                if (!isAdjusted) {
                    System.out.println("Gagal adjust di delete pembelian");
                    return false;
                }
            }
            old.setDeletedAt(getCurrentTimestamp());
            purchasingDetailRepository.save(old);
        }

        transactionEntity.setDeletedAt(getCurrentTimestamp());
        purchasingRepository.save(transactionEntity);

        return true;
    }

    @Transactional
    public ResponseInBoolean createTransaction(CreatePurchasingRequest req, ClientEntity clientData){
        System.out.println("req : " + req);
        //Cek no faktur
        Optional<PurchasingEntity> pembelian = purchasingRepository.findFirstByPurchasingNumberAndClientEntity_ClientIdAndDeletedAtIsNull(req.getPurchasingNumber(), clientData.getClientId());
        if(pembelian.isPresent()){
            return new ResponseInBoolean(false, "Nomor Faktur Sudah Ada");
        }
        try{
            //Get Supplier Entity
            Optional<SupplierEntity> supplierEntity = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientData.getClientId());
            if (supplierEntity.isEmpty()){
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
            if(!req.isCash()){
                purchasingEntity.setPoDueDate(LocalDate.parse(req.getPoDueDate()).atStartOfDay());
            }
            purchasingEntity.setCash(req.isCash());
            //Assign isPaid with isCash
            purchasingEntity.setPaid(req.isCash());
            purchasingEntity.setClientEntity(clientData);
            purchasingEntity.setSubtotal(req.getSubtotal());
            purchasingRepository.save(purchasingEntity);
            System.out.println("Created transaction : " + newPurchasingId);

            //Insert all the transaction details
            Long lastTransactionDetailId = purchasingDetailRepository.findFirstByDeletedAtIsNullOrderByPurchasingDetailIdDesc().map(PurchasingDetailEntity::getPurchasingDetailId).orElse(0L);
            Long newPurchasingDetailId = Generator.generateId(lastTransactionDetailId);

            for(PurchasingDetailDTO dtos : req.getPembelianDetailDTOS()){
                ProductEntity productEntity = productRepository.findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(dtos.getName(), dtos.getCode(), clientData.getClientId());

                PurchasingDetailEntity purchasingDetailEntity = new PurchasingDetailEntity();
                purchasingDetailEntity.setPurchasingDetailId(newPurchasingDetailId);
                purchasingDetailEntity.setShortName(dtos.getCode());
                purchasingDetailEntity.setFullName(dtos.getName());
                purchasingDetailEntity.setQty(dtos.getQty());
                purchasingDetailEntity.setPrice(dtos.getPrice());
                purchasingDetailEntity.setDiscAmount(dtos.getDiscAmount());
                purchasingDetailEntity.setTotalPrice(dtos.getTotal());
                purchasingDetailEntity.setPurchasingEntity(purchasingEntity);

                int counter = 0;
                for(ProductPricesEntity productPriceData : productEntity.getProductPricesEntity()){
                    counter++;
                    if(productPriceData.getPrice() != null && productPriceData.getPrice().compareTo(BigDecimal.ZERO) > 0){
                        if(counter == 1){
                            purchasingDetailEntity.setMarkup1(dtos.getMarkup1());
                            purchasingDetailEntity.setHargaJual1(dtos.getHargaJual1());
                        }else if(counter == 2){
                            purchasingDetailEntity.setMarkup2(dtos.getMarkup2());
                            purchasingDetailEntity.setHargaJual2(dtos.getHargaJual2());
                        }else if(counter == 3){
                            purchasingDetailEntity.setMarkup3(dtos.getMarkup3());
                            purchasingDetailEntity.setHargaJual3(dtos.getHargaJual3());
                        }
                    }
                }
                purchasingDetailRepository.save(purchasingDetailEntity);
                newPurchasingDetailId = Generator.generateId(newPurchasingDetailId);

                //Update product stock
                Long newStock = productEntity.getStock() + dtos.getQty();
                productEntity.setStock(newStock);
                productRepository.save(productEntity);

                //Insert kartu stok
                boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                        productEntity,
                        req.getPurchasingNumber(),
                        TipeKartuStok.PEMBELIAN,
                        dtos.getQty(),
                        0L,
                        newStock,
                        clientData
                ));
                if (!isAdjusted) {
                    return new ResponseInBoolean(false, "Gagal adjust di create pembelian");
                }
            }
            return new ResponseInBoolean(true, "Berhasil simpan data");
        }catch (Exception e){
            System.out.println("Exception catched : " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return new ResponseInBoolean(false, e.getMessage());
        }
    }

    @Transactional
    public ResponseInBoolean editTransaction(Long purchasingId, CreatePurchasingRequest req, ClientEntity clientData){
        //Check duplication
        boolean pembelianDuplicate = purchasingRepository.existsByPurchasingNumberAndClientEntity_ClientIdAndDeletedAtIsNullAndPurchasingIdNot(req.getPurchasingNumber(), clientData.getClientId(), purchasingId);
        System.out.println("Pembelian duplikat ?  : " + pembelianDuplicate);
        if(pembelianDuplicate){
            return new ResponseInBoolean(false, "Nomor faktur sudah ada");
        }
        try{
            //Get Supplier Entity
            System.out.println("otw get supplier");
            Optional<SupplierEntity> supplierEntityOpt = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientData.getClientId());
            if (supplierEntityOpt.isEmpty()){
                System.out.println("supplier ga nemu");
                return new ResponseInBoolean(false, "Supplier tidak ada");
            }
            SupplierEntity supplierEntity = supplierEntityOpt.get();

            System.out.println("otw get purchasing");

            //Check if transaction exist
            Optional<PurchasingEntity> purchasingEntityOpt = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(clientData.getClientId(), purchasingId);
            if(purchasingEntityOpt.isEmpty()){
                System.out.println("purchasing ga nemu");
                return new ResponseInBoolean(false, "Data Pembelian tidak ada");
            }
            PurchasingEntity purchasingEntity = purchasingEntityOpt.get();

            System.out.println("otw set purchasing");

            //insert the transaction data
            purchasingEntity.setPurchasingNumber(req.getPurchasingNumber());
            purchasingEntity.setSupplierEntity(supplierEntity);
            purchasingEntity.setTotalPrice(req.getTotalPrice());
            purchasingEntity.setTotalDiscount(req.getTotalDisc());
            purchasingEntity.setPoDate(LocalDate.parse(req.getPoDate()).atStartOfDay());
            if(!req.isCash()){
                purchasingEntity.setPoDueDate(LocalDate.parse(req.getPoDueDate()).atStartOfDay());
            }
            purchasingEntity.setCash(req.isCash());
            purchasingEntity.setSubtotal(req.getSubtotal());
            purchasingRepository.save(purchasingEntity);

            //Restore stock from old transaction
            System.out.println("otw get old transaction");

            List<PurchasingDetailEntity> oldTransactions = purchasingDetailRepository.findAllByPurchasingEntity_PurchasingIdOrderByPurchasingDetailIdDesc(purchasingId);
            System.out.println("Old transaction IDs:");
            for (PurchasingDetailEntity old : oldTransactions) {
                System.out.println("→ ID: " + old.getPurchasingDetailId() + ", Name: " + old.getFullName() + ", Qty: " + old.getQty());
            }
            for(PurchasingDetailEntity old : oldTransactions){
                ProductEntity product = productRepository.findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(old.getFullName(), old.getShortName(), clientData.getClientId());
                if(product != null){
                    System.out.println("otw update product : " + product.getShortName());
                    Long restoredStock = product.getStock() - old.getQty();
                    product.setStock(restoredStock);
                    productRepository.save(product);

                    System.out.println("Otw insert kartu stok di restoring product stock di edit pembelian");
                    if (!Objects.equals(product.getStock(), old.getQty())) {
                        boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                                product,
                                purchasingEntity.getPurchasingNumber(),
                                TipeKartuStok.KOREKSI_PEMBELIAN,
                                0L,
                                old.getQty(),
                                restoredStock,
                                clientData
                        ));
                        if (!isAdjusted) {
                            System.out.println("Gagal adjust di edit pembelian (1)");
                            return new ResponseInBoolean(false, "Gagal adjust di edit pembelian");
                        }
                        System.out.println("Sukses adjust di edit pembelian (1)");
                    }
                }
            }

            System.out.println("otw delete old transaction");

            //Delete all product prices related to product id
            purchasingDetailRepository.deleteAllByPurchasingEntity_PurchasingId(purchasingId);

            //Insert all the transaction details
            Long lastPurchasingDetailId = purchasingDetailRepository.findFirstByDeletedAtIsNullOrderByPurchasingDetailIdDesc().map(PurchasingDetailEntity::getPurchasingDetailId).orElse(0L);
            Long newPurchasingDetailId = Generator.generateId(lastPurchasingDetailId);

            for(PurchasingDetailDTO dtos : req.getPembelianDetailDTOS()){
                if(dtos != null) {
                    ProductEntity productEntity = productRepository.findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(dtos.getName(), dtos.getCode(), clientData.getClientId());
                    PurchasingDetailEntity purchasingDetailEntity = new PurchasingDetailEntity();
                    purchasingDetailEntity.setPurchasingDetailId(newPurchasingDetailId);
                    purchasingDetailEntity.setShortName(dtos.getCode());
                    purchasingDetailEntity.setFullName(dtos.getName());
                    purchasingDetailEntity.setQty(dtos.getQty());
                    purchasingDetailEntity.setPrice(dtos.getPrice());
                    purchasingDetailEntity.setDiscAmount(dtos.getDiscAmount());
                    purchasingDetailEntity.setTotalPrice(dtos.getTotal());
                    purchasingDetailEntity.setPurchasingEntity(purchasingEntity);

                    int counter = 0;
                    for(ProductPricesEntity productPriceData : productEntity.getProductPricesEntity()){
                        counter++;
                        if(productPriceData.getPrice() != null && productPriceData.getPrice().compareTo(BigDecimal.ZERO) > 0){
                            if(counter == 1){
                                purchasingDetailEntity.setMarkup1(dtos.getMarkup1());
                                purchasingDetailEntity.setHargaJual1(dtos.getHargaJual1());
                            }else if(counter == 2){
                                purchasingDetailEntity.setMarkup2(dtos.getMarkup2());
                                purchasingDetailEntity.setHargaJual2(dtos.getHargaJual2());
                            }else if(counter == 3){
                                purchasingDetailEntity.setMarkup3(dtos.getMarkup3());
                                purchasingDetailEntity.setHargaJual3(dtos.getHargaJual3());
                            }
                        }
                    }
                    purchasingDetailRepository.save(purchasingDetailEntity);
                    newPurchasingDetailId = Generator.generateId(newPurchasingDetailId);

                    //Update product stock
                    Long newStock = productEntity.getStock() + dtos.getQty();
                    productEntity.setStock(newStock);
                    productRepository.save(productEntity);

                    // cari di snapshot lama
                    PurchasingDetailEntity oldDetail = oldTransactions.stream()
                            .filter(o -> o.getShortName().equals(dtos.getCode()) || o.getFullName().equals(dtos.getName()))
                            .findFirst()
                            .orElse(null);

                    if (oldDetail != null && Objects.equals(oldDetail.getQty(), dtos.getQty())) {
                        System.out.println("Qty tidak berubah, skip kartu stok untuk product " + dtos.getName());
                        continue;
                    }
                    boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                            productEntity,
                            purchasingEntity.getPurchasingNumber(),
                            TipeKartuStok.KOREKSI_PEMBELIAN,
                            dtos.getQty(),
                            0L,
                            newStock,
                            clientData
                    ));
                    if (!isAdjusted) {
                        System.out.println("Gagal adjust di edit product (2)");
                        return new ResponseInBoolean(false, "Gagal adjust di create transaction (2)");
                    }
                }
            }
            return new ResponseInBoolean(true, "Data berhasil disimpan");
        }catch (Exception e){
            System.out.println("Exception catched : " + e);
            return new ResponseInBoolean(false, e.getMessage());
        }
    }

    @Transactional
    public boolean payFaktur(Long clientId, Long pembelianId){
        try{
            Optional<PurchasingEntity> purchasingEntity = purchasingRepository.findFirstByClientEntity_ClientIdAndPurchasingIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, pembelianId);
            if(purchasingEntity.isEmpty()){
                return false;
            }
            PurchasingEntity purchasingData = purchasingEntity.get();
            purchasingData.setPaid(true);
            purchasingRepository.save(purchasingData);
            return true;
        }catch (Exception e){
            System.out.println("Error : " + e);
            e.printStackTrace();
            return false;
        }
    }

}
