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

import static com.pos.posApps.Util.Generator.formatPhoneTo62;
import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class IndenService {
    @Autowired
    ProductRepository productRepository;

    @Autowired
    IndenRepository indenRepository;

    @Autowired
    KasirService kasirService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    IndenDetailRepository indenDetailRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    TransactionDetailRepository transactionDetailRepository;

    @Autowired
    StockMovementService stockMovementService;

    public Page<IndenDTO> getIndenData(String statusInden, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<IndenEntity> indenData;
        if (statusInden != null) {
            indenData = indenRepository.findAllByDeletedAtIsNullAndStatusIndenAndCreatedAtBetweenOrderByCreatedAtDesc(statusInden, startDate, endDate, pageable);
        } else {
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
        if (indens.getAccountEntity() != null) {
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
        if (indenEntityOpt.isEmpty()) {
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
    public ResponseForWhatsapp updateStatusInden(Long indenId, String newStatusInden, AccountEntity accountData) {
        try {
            boolean isOpenWa = false;
            ClientEntity clientData = accountData.getClientEntity();
            Optional<IndenEntity> indenEntityOpt = indenRepository.findFirstByIndenIdAndDeletedAtIsNull(indenId);
            if (indenEntityOpt.isEmpty()) {
                return new ResponseForWhatsapp(false, "Data Inden tidak ditemukan", isOpenWa, "", "");
            }

            IndenEntity indenEntity = indenEntityOpt.get();

            indenEntity.setStatusInden(newStatusInden);
            indenRepository.save(indenEntity);

            //Insert into transaction

            if (newStatusInden.equalsIgnoreCase(StatusInden.DISERAHKAN.name())) {
                String lastProduct = "Tanya Leon";
                List<IndenDetailEntity> indenDetailEntities = indenDetailRepository.findAllByIndenEntity_IndenIdAndDeletedAtIsNullOrderByIndenDetailIdDesc(indenId);
                if (indenDetailEntities.isEmpty()) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return new ResponseForWhatsapp(false, "Data Inden detail tidak ditemukan", isOpenWa, "", "");
                }

                //Get Customer Data (Umum = 1)
                CustomerEntity customerData = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(1L, clientData.getClientId()).get();

                //Insert into transaction
                TransactionEntity transactionEntity = new TransactionEntity();
                transactionEntity.setClientEntity(clientData);
                transactionEntity.setTransactionNumber(indenEntity.getIndenNumber());
                transactionEntity.setCustomerEntity(customerData);
                transactionEntity.setTotalPrice(indenEntity.getTotalPrice());
                transactionEntity.setTotalDiscount(indenEntity.getTotalDiscount());
                transactionEntity.setSubtotal(indenEntity.getSubtotal());
                transactionEntity.setAccountEntity(accountData);
                transactionRepository.save(transactionEntity);

                for (IndenDetailEntity dtos : indenDetailEntities) {
                    System.out.println("Part Number : " + dtos.getShortName());
                    System.out.println("Nama Barang : " + dtos.getFullName());

                    //Get product Entity
                    ProductEntity productEntity = productRepository.findAndLockProduct(dtos.getFullName(), dtos.getShortName(), clientData.getClientId());
                    entityManager.refresh(productEntity);

                    if (productEntity == null) {
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        return new ResponseForWhatsapp(true, "Produk " + dtos.getFullName() + " tidak ditemukan", isOpenWa, "", "");
                    }
                    System.out.println("Produk: " + productEntity.getShortName() + "(" + productEntity.getStock() + ") VALID");
                    lastProduct = dtos.getShortName();
                    TransactionDetailEntity transactionDetailEntity = new TransactionDetailEntity();
                    transactionDetailEntity.setShortName(dtos.getShortName());
                    transactionDetailEntity.setFullName(dtos.getFullName());
                    transactionDetailEntity.setQty(dtos.getQty());
                    transactionDetailEntity.setPrice(dtos.getPrice());
                    transactionDetailEntity.setDiscountAmount(dtos.getDiscountAmount());
                    transactionDetailEntity.setTotalPrice(dtos.getTotalPrice());
                    transactionDetailEntity.setTransactionEntity(transactionEntity);
                    transactionDetailEntity.setBasicPrice(dtos.getBasicPrice());
                    transactionDetailEntity.setTotalProfit(dtos.getTotalProfit());
                    transactionDetailRepository.save(transactionDetailEntity);

                    //Update product stock
                    System.out.println("Stock Before : " + productEntity.getStock());
                    System.out.println("Qty : " + dtos.getQty());
                    Long newStock = productEntity.getStock() - dtos.getQty();
                    productEntity.setStock(newStock);
                    productRepository.save(productEntity);
                    stockMovementService.insertKartuStok(new AdjustStockDTO(
                            productEntity,
                            indenEntity.getIndenNumber(),
                            TipeKartuStok.PENJUALAN,
                            0L,
                            dtos.getQty(),
                            newStock,
                            clientData
                    ));
                    System.out.println("Stock After : " + newStock);
                    System.out.println();
                }
            }
            String phoneNumber = formatPhoneTo62(indenEntity.getCustomerPhone());
            StringBuilder message = new StringBuilder();
            List<IndenDetailEntity> indenDetailEntities = indenDetailRepository.findAllByIndenEntity_IndenIdAndDeletedAtIsNullOrderByIndenDetailIdDesc(indenId);
            if (newStatusInden.equalsIgnoreCase(StatusInden.TERCATAT.name())) {
                isOpenWa = true;
                message.append("Halo, kak ").append(indenEntity.getCustomerName()).append(".\n\n")
                        .append("Terima kasih telah melakukan pemesanan dengan nomor pesanan (").append(indenEntity.getIndenNumber()).append(") di Anugrah Motor Tanjung Enim.\n")
                        .append("Detail Pesanan:\n");
                int no = 1;
                for (IndenDetailEntity item : indenDetailEntities) {
                    message.append(no).append(") ")
                            .append(item.getShortName()).append(" | ")
                            .append(item.getFullName()).append(" | ")
                            .append(item.getQty()).append(" buah")
                            .append("\n");
                    no++;
                }
                message.append("\nPesanan Anda telah masuk ke sistem kami. Mohon menunggu info selanjutnya.\n\n");
                message.append("--Pesan ini dibuat secara otomatis--");
            } else if (newStatusInden.equalsIgnoreCase(StatusInden.KOSONG.name())) {
                isOpenWa = true;
                message.append("Halo, kak ").append(indenEntity.getCustomerName()).append(".\n\n")
                        .append("Kami dari Anugrah Motor Tanjung Enim menyampaikan permohonan maaf terkait pesanan nomor ").append(indenEntity.getIndenNumber()).append(".\n\n");
                message.append("Saat ini, pesanan Anda tidak dapat kami proses dikarenakan stok barang tersebut sedang kosong. Sehubungan dengan hal tersebut, mohon kesediaan Anda untuk datang ke toko kami guna proses pengembalian deposit (refund).");
                message.append("\n\nTerima kasih atas pengertiannya.\n\n");
                message.append("--Pesan ini dibuat secara otomatis--");
            } else if (newStatusInden.equalsIgnoreCase(StatusInden.DITERIMA.name())) {
                isOpenWa = true;
                message.append("Halo, kak ").append(indenEntity.getCustomerName()).append(".\n\n")
                        .append("Terima kasih telah melakukan pemesanan dengan nomor pesanan (").append(indenEntity.getIndenNumber()).append(") di Anugrah Motor Tanjung Enim.\n");
                message.append("Kami ingin menginformasikan bahwa pesanan anda telah tiba dan sudah tersedia di toko kami.");
                message.append("\n\nSilakan datang ke lokasi kami untuk pengambilan barang. Terima kasih\n\n");
                message.append("--Pesan ini dibuat secara otomatis--");
            }
            return new ResponseForWhatsapp(true, "Berhasil memperbarui status data inden.", isOpenWa, phoneNumber, message.toString());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseForWhatsapp(false, e.getMessage(), false, "", "");
        }

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
