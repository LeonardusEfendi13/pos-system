package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.StatusInden;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import com.pos.posApps.Util.SalePaymentRules;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import static com.pos.posApps.Util.Generator.*;

@Service
public class IndenService {
    @Autowired
    ProductRepository productRepository;

    @Autowired
    IndenRepository indenRepository;

    @Autowired
    KasirService kasirService;

    @Autowired
    PaymentMethodService paymentMethodService;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    BuktiBayarRepository buktiBayarRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    IndenDetailRepository indenDetailRepository;

    @Autowired
    CustomerRepository customerRepository;

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
                balanceOf(indens.getTotalPrice(), indens.getPaidAmount()),
                name,
                indens.getCustomerName(),
                indens.getCustomerPhone(),
                indens.getStatusInden(),
                indens.isCash(),
                indens.isPaid(),
                indens.getPaidAmount(),
                indens.getDueDate(),
                indens.getPaymentMethodId(),
                indens.getPaymentMethodName(),
                indens.getPaymentMethodType(),
                indens.getPaymentMethodRekening()
        );
    }

    private static BigDecimal balanceOf(BigDecimal totalPrice, BigDecimal paidAmount) {
        BigDecimal total = totalPrice == null ? BigDecimal.ZERO : totalPrice;
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal balance = total.subtract(paid);
        return balance.signum() < 0 ? BigDecimal.ZERO : balance;
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
    public ResponseForWhatsapp updateStatusInden(
            Long indenId,
            String newStatusInden,
            Long paymentMethodId,
            MultipartFile buktiPembayaran,
            AccountEntity accountData,
            ClientEntity clientEntity
    ) {
        try {
            boolean isOpenWa = false;
            ClientEntity clientData = accountData.getClientEntity();
            Optional<IndenEntity> indenEntityOpt = indenRepository.findFirstByIndenIdAndDeletedAtIsNull(indenId);
            if (indenEntityOpt.isEmpty()) {
                return new ResponseForWhatsapp(false, "Data Inden tidak ditemukan", isOpenWa, "", "");
            }

            IndenEntity indenEntity = indenEntityOpt.get();
            boolean alreadyHandedOver = StatusInden.DISERAHKAN.name()
                    .equalsIgnoreCase(indenEntity.getStatusInden());
            boolean saleAlreadyPosted = indenEntity.isSalePosted() || alreadyHandedOver;
            if (saleAlreadyPosted) {
                indenEntity.setSalePosted(true);
            }

            indenEntity.setStatusInden(newStatusInden);
            indenRepository.save(indenEntity);

            //Insert into transaction

            if (newStatusInden.equalsIgnoreCase(StatusInden.DISERAHKAN.name()) && !saleAlreadyPosted) {
                String lastProduct = "Tanya Leon";
                List<IndenDetailEntity> indenDetailEntities = indenDetailRepository.findAllByIndenEntity_IndenIdAndDeletedAtIsNullOrderByIndenDetailIdDesc(indenId);
                if (indenDetailEntities.isEmpty()) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return new ResponseForWhatsapp(false, "Data Inden detail tidak ditemukan", isOpenWa, "", "");
                }

                BigDecimal totalPrice = indenEntity.getTotalPrice() == null
                        ? BigDecimal.ZERO
                        : indenEntity.getTotalPrice();
                BigDecimal paidSoFar = indenEntity.getPaidAmount() == null
                        ? BigDecimal.ZERO
                        : indenEntity.getPaidAmount();
                BigDecimal remainder = totalPrice.subtract(paidSoFar);
                if (remainder.signum() < 0) {
                    remainder = BigDecimal.ZERO;
                }

                PaymentMethodEntity handoverMethod = null;
                if (remainder.signum() > 0) {
                    if (paymentMethodId == null) {
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        return new ResponseForWhatsapp(false, "Metode pembayaran wajib dipilih", isOpenWa, "", "");
                    }
                    handoverMethod = paymentMethodService.resolve(
                            clientData.getClientId(),
                            paymentMethodId,
                            false
                    );
                    if (handoverMethod == null) {
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        return new ResponseForWhatsapp(false, "Metode pembayaran tidak ditemukan", isOpenWa, "", "");
                    }
                } else if (indenEntity.getPaymentMethodId() != null) {
                    handoverMethod = paymentMethodService.resolve(
                            clientData.getClientId(),
                            indenEntity.getPaymentMethodId(),
                            false
                    );
                }

                boolean cash = handoverMethod == null
                        ? indenEntity.isCash()
                        : "cash".equalsIgnoreCase(handoverMethod.getMethodType());
                indenEntity.setCash(cash);
                indenEntity.setPaid(true);
                indenEntity.setPaidAmount(totalPrice);
                indenEntity.setDueDate(null);
                if (handoverMethod != null) {
                    paymentMethodService.copyToInden(indenEntity, handoverMethod);
                }
                indenEntity.setSalePosted(true);
                indenRepository.save(indenEntity);

                CustomerEntity customerData = resolveHandoverCustomer(indenEntity, clientData);

                String generatedNotaNumber = kasirService.generateTodayNota(clientData.getClientId());

                //Insert into transaction
                TransactionEntity transactionEntity = new TransactionEntity();
                transactionEntity.setClientEntity(clientData);
                transactionEntity.setTransactionNumber(generatedNotaNumber);
                transactionEntity.setCustomerEntity(customerData);
                transactionEntity.setTotalPrice(indenEntity.getTotalPrice());
                transactionEntity.setTotalDiscount(indenEntity.getTotalDiscount());
                transactionEntity.setSubtotal(indenEntity.getSubtotal());
                transactionEntity.setAccountEntity(accountData);
                transactionEntity.setCash(cash);
                transactionEntity.setPaid(true);
                transactionEntity.setPaidAmount(totalPrice);
                transactionEntity.setDueDate(null);
                if (handoverMethod != null) {
                    paymentMethodService.copyToTransaction(transactionEntity, handoverMethod);
                } else {
                    transactionEntity.setPaymentMethodId(indenEntity.getPaymentMethodId());
                    transactionEntity.setPaymentMethodName(indenEntity.getPaymentMethodName());
                    transactionEntity.setPaymentMethodType(indenEntity.getPaymentMethodType());
                    transactionEntity.setPaymentMethodRekening(indenEntity.getPaymentMethodRekening());
                }
                transactionRepository.save(transactionEntity);
                storeTransferBukti(
                        indenEntity,
                        clientData.getClientId(),
                        handoverMethod == null ? null : handoverMethod.getMethodType(),
                        buktiPembayaran
                );
                indenRepository.save(indenEntity);
                saveIndenBukti(indenEntity, transactionEntity);

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
                    transactionDetailEntity.setProductId(productEntity.getProductId());
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
                            generatedNotaNumber,
                            TipeKartuStok.PENJUALAN,
                            0L,
                            dtos.getQty(),
                            newStock,
                            clientData,
                            getCurrentTimestamp()
                    ));
                    System.out.println("Stock After : " + newStock);
                    System.out.println();
                }
            }
            String phoneNumber = formatPhoneTo62(indenEntity.getCustomerPhone());
            StringBuilder message = new StringBuilder();
            List<IndenDetailEntity> indenDetailEntities = indenDetailRepository.findAllByIndenEntity_IndenIdAndDeletedAtIsNullOrderByIndenDetailIdDesc(indenId);
            String namaToko = clientEntity.getName();
            String kota = clientEntity.getKota();
            if (newStatusInden.equalsIgnoreCase(StatusInden.TERCATAT.name())) {
                isOpenWa = true;
                message.append(buildTercatatWhatsappMessage(
                        indenEntity,
                        namaToko,
                        kota,
                        indenDetailEntities
                ));
            } else if (newStatusInden.equalsIgnoreCase(StatusInden.KOSONG.name())) {
                isOpenWa = true;
                message.append("Halo, kak ").append(indenEntity.getCustomerName()).append(".\n\n")
                        .append("Kami dari ").append(namaToko).append(" ").append(kota).append(" menyampaikan permohonan maaf terkait pesanan nomor ").append(indenEntity.getIndenNumber()).append(".\n\n");
                message.append("Saat ini, pesanan Anda tidak dapat kami proses dikarenakan stok barang tersebut sedang kosong. Sehubungan dengan hal tersebut, mohon kesediaan Anda untuk datang ke toko kami guna proses pengembalian deposit (refund).");
                message.append("\n\nTerima kasih atas pengertiannya.\n\n");
                message.append("--Pesan ini dibuat secara otomatis--");
            } else if (newStatusInden.equalsIgnoreCase(StatusInden.DITERIMA.name())) {
                isOpenWa = true;
                message.append("Halo, kak ").append(indenEntity.getCustomerName()).append(".\n\n")
                        .append("Terima kasih telah melakukan pemesanan dengan nomor pesanan (").append(indenEntity.getIndenNumber()).append(") di ").append(namaToko).append(" ").append(kota).append(".\n\n");
                message.append("Kami ingin menginformasikan bahwa pesanan anda telah tiba dan sudah tersedia di toko kami.");
                message.append("\n\nSilakan datang ke toko kami untuk pengambilan barang. Terima kasih\n\n");
                message.append("--Pesan ini dibuat secara otomatis--");
            }
            return new ResponseForWhatsapp(true, "Berhasil memperbarui status data inden.", isOpenWa, phoneNumber, message.toString());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            System.out.println("Ada apa ni bang : " + e.getMessage());
            return new ResponseForWhatsapp(false, e.getMessage(), false, "", "");
        }
    }

    private String buildTercatatWhatsappMessage(
            IndenEntity indenEntity,
            String namaToko,
            String kota,
            List<IndenDetailEntity> details
    ) {
        StringBuilder message = new StringBuilder();
        message.append("Halo, kak ").append(indenEntity.getCustomerName()).append(".\n\n")
                .append("Terima kasih telah melakukan pemesanan dengan nomor pesanan (").append(indenEntity.getIndenNumber()).append(") di ").append(namaToko).append(" ").append(kota).append(".\n\n")
                .append("Total pesanan : ").append(formatRupiah(indenEntity.getTotalPrice())).append("\n")
                .append("Deposit : ").append(formatRupiah(indenEntity.getDeposit())).append("\n")
                .append("Sisa pembayaran : ").append(formatRupiah(indenEntity.getTotalPrice().subtract(indenEntity.getDeposit()))).append("\n\n")
                .append("Detail Pesanan:\n");
        int no = 1;
        for (IndenDetailEntity item : details) {
            message.append(no).append(") ")
                    .append(item.getShortName()).append(" | ")
                    .append(item.getFullName()).append(" | ")
                    .append(item.getQty()).append(" buah")
                    .append("\n");
            no++;
        }
        message.append("\nPesanan Anda telah masuk ke sistem kami. Mohon menunggu info selanjutnya.\n\n");
        message.append("--Pesan ini dibuat secara otomatis--");
        return message.toString();
    }

    @Transactional
    public ResponseForWhatsapp createTransaction(CreateIndenRequest req, AccountEntity accountData) {
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
            String paymentError = applyIndenPayment(
                    indenEntity,
                    req,
                    clientData.getClientId(),
                    null,
                    null,
                    LocalDate.now()
            );
            if (paymentError != null) {
                return new ResponseForWhatsapp(false, paymentError, false, "", "");
            }
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
                    return new ResponseForWhatsapp(true, "Produk " + dtos.getName() + " tidak ditemukan", false, "", "");
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

            String phoneNumber = formatPhoneTo62(indenEntity.getCustomerPhone());
            List<IndenDetailEntity> details = indenDetailRepository
                    .findAllByIndenEntity_IndenIdAndDeletedAtIsNullOrderByIndenDetailIdDesc(indenEntity.getIndenId());
            String waMessage = buildTercatatWhatsappMessage(
                    indenEntity,
                    clientData.getName(),
                    clientData.getKota(),
                    details
            );
            boolean openWa = phoneNumber != null && !phoneNumber.isBlank();
            return new ResponseForWhatsapp(
                    true,
                    generatedNotaNumber,
                    openWa,
                    phoneNumber == null ? "" : phoneNumber,
                    waMessage
            );
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseForWhatsapp(false, e.getMessage() + ". ERROR KARENA : " + lastProduct, false, "", "");
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
            LocalDate invoiceDate = inden.getCreatedAt() == null
                    ? LocalDate.now()
                    : inden.getCreatedAt().toLocalDate();
            String paymentError = applyIndenPayment(
                    inden,
                    req,
                    clientData.getClientId(),
                    inden.isCash(),
                    inden.isPaid(),
                    invoiceDate
            );
            if (paymentError != null) {
                return new ResponseInBoolean(false, paymentError);
            }
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

    private String applyIndenPayment(
            IndenEntity inden,
            CreateIndenRequest request,
            Long clientId,
            Boolean existingCash,
            Boolean existingPaid,
            LocalDate invoiceDate
    ) {
        SalePaymentRules.Decision payment = SalePaymentRules.resolve(
                false,
                request.getIsCash(),
                request.getDeposit(),
                request.getTotalPrice(),
                request.getDueDate(),
                existingCash,
                existingPaid,
                invoiceDate,
                LocalDate.now()
        );
        if (!payment.ok()) {
            return payment.error();
        }

        inden.setDeposit(payment.paidAmount());
        inden.setCash(payment.cash());
        inden.setPaid(payment.paid());
        inden.setPaidAmount(payment.paidAmount());
        inden.setDueDate(payment.dueDate());

        if (!payment.cash()) {
            if (!payment.paid()) {
                clearIndenPaymentMethod(inden);
            }
            return null;
        }

        PaymentMethodEntity method = paymentMethodService.resolve(
                clientId,
                request.getPaymentMethodId(),
                false
        );
        if (method == null) {
            return "Metode pembayaran tidak ditemukan";
        }

        paymentMethodService.copyToInden(inden, method);
        return null;
    }

    private void clearIndenPaymentMethod(IndenEntity inden) {
        inden.setPaymentMethodId(null);
        inden.setPaymentMethodName(null);
        inden.setPaymentMethodType(null);
        inden.setPaymentMethodRekening(null);
    }

    @Transactional
    public ResponseInBoolean payInden(
            Long clientId,
            Long indenId,
            Long paymentMethodId,
            MultipartFile buktiPembayaran
    ) {
        try {
            IndenEntity inden = indenRepository.findFirstByIndenIdAndDeletedAtIsNull(indenId).orElse(null);
            if (inden == null) {
                return new ResponseInBoolean(false, "Data inden tidak ditemukan");
            }

            if (inden.isCash() || inden.isPaid()) {
                return new ResponseInBoolean(false, "Inden tidak bisa dilunaskan");
            }

            PaymentMethodEntity method = paymentMethodService.resolve(clientId, paymentMethodId, false);
            if (method == null) {
                return new ResponseInBoolean(false, "Metode pembayaran tidak ditemukan");
            }

            storeTransferBukti(inden, clientId, method.getMethodType(), buktiPembayaran);

            paymentMethodService.copyToInden(inden, method);
            inden.setPaid(true);
            inden.setPaidAmount(inden.getTotalPrice() == null ? BigDecimal.ZERO : inden.getTotalPrice());
            inden.setDeposit(inden.getPaidAmount());
            indenRepository.save(inden);

            transactionRepository
                    .findFirstByClientEntity_ClientIdAndTransactionNumberAndDeletedAtIsNull(
                            clientId,
                            inden.getIndenNumber()
                    )
                    .ifPresent(transaction -> {
                        paymentMethodService.copyToTransaction(transaction, method);
                        transaction.setPaid(true);
                        transaction.setPaidAmount(inden.getPaidAmount());
                        transactionRepository.save(transaction);
                        saveIndenBukti(inden, transaction);
                    });

            return new ResponseInBoolean(true, "Inden berhasil dilunaskan");
        } catch (Exception exception) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Terjadi kesalahan: " + exception.getMessage());
        }
    }

    private CustomerEntity resolveHandoverCustomer(IndenEntity inden, ClientEntity client) {
        String name = inden.getCustomerName() == null ? "" : inden.getCustomerName().trim();
        if (!name.isEmpty()) {
            CustomerEntity matched = customerRepository
                    .findByNameAndDeletedAtIsNullAndClientEntity_ClientId(name, client.getClientId());
            if (matched != null) {
                return matched;
            }
        }

        return customerRepository
                .findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(1L, client.getClientId())
                .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));
    }

    private void storeTransferBukti(
            IndenEntity inden,
            Long clientId,
            String methodType,
            MultipartFile buktiPembayaran
    ) throws java.io.IOException {
        if (buktiPembayaran == null || buktiPembayaran.isEmpty()) {
            return;
        }
        if (!"transfer".equalsIgnoreCase(methodType)) {
            return;
        }

        String originalName = buktiPembayaran.getOriginalFilename();
        String uploadDir = "uploads/bukti/" + clientId + "/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = System.currentTimeMillis() + "_" + originalName;
        Path path = Paths.get(uploadDir + fileName);
        Files.copy(buktiPembayaran.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        inden.setBuktiOriginalName(originalName);
        inden.setBuktiFilePath(uploadDir + fileName);
    }

    private void saveIndenBukti(IndenEntity inden, TransactionEntity transaction) {
        if (inden.getBuktiFilePath() == null || inden.getBuktiFilePath().isBlank()) {
            return;
        }

        if (buktiBayarRepository.findByTransactionEntity_TransactionId(transaction.getTransactionId()).isPresent()) {
            return;
        }

        BuktiBayarEntity bukti = new BuktiBayarEntity();
        bukti.setOriginalName(inden.getBuktiOriginalName());
        bukti.setFilePath(inden.getBuktiFilePath());
        bukti.setTransactionEntity(transaction);
        bukti.setRekeningAsal("");
        bukti.setRekeningTujuan(inden.getPaymentMethodRekening() == null ? "" : inden.getPaymentMethodRekening());
        bukti.setJenisBayar(inden.getPaymentMethodType());
        buktiBayarRepository.save(bukti);
    }
}
