package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

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

    public BigDecimal getTotalRevenues(Long clientId) {
        LocalDateTime startDate = LocalDate.now().atStartOfDay();
        LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);
        List<TransactionEntity> transactionData = transactionRepository.findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate).stream().toList();
        return transactionData.stream()
                .map(TransactionEntity::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<LaporanPerWaktuDTO> getLaporanPenjualanDataByPeriode(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long customerId,
            String filterOptions
    ) {
        List<TransactionEntity> transactionData;

        if (customerId == null) {
            transactionData = transactionRepository
                    .findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(
                            clientId, startDate, endDate);
        } else {
            transactionData = transactionRepository
                    .findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(
                            clientId, customerId, startDate, endDate);
        }

        DateTimeFormatter formatter = switch (filterOptions != null ? filterOptions.toLowerCase() : "") {
            case "year" -> DateTimeFormatter.ofPattern("yyyy");
            case "month" -> DateTimeFormatter.ofPattern("yyyy-MM");
            default -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
        };

        // Generate all periods between start and end
        List<String> allPeriods = generateAllPeriods(startDate.toLocalDate(), endDate.toLocalDate(), filterOptions);

        // Preload semua produk (asumsikan shortName unik)
        Map<String, ProductEntity> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(ProductEntity::getShortName, Function.identity(), (a, b) -> a));

        // Grouping by actual data
        Map<String, LaporanPerWaktuDTO> groupedMap = transactionData.stream()
                .flatMap(trx -> trx.getTransactionDetailEntities().stream()
                        .filter(detail -> detail.getDeletedAt() == null)
                        .map(detail -> {
                            String period = trx.getCreatedAt().format(formatter);
                            BigDecimal hargaJual = detail.getPrice();
                            BigDecimal totalPrice = detail.getTotalPrice();
                            Long qty = detail.getQty();

                            ProductEntity product = productMap.get(detail.getShortName());
                            BigDecimal hargaBeli = (product != null) ? product.getSupplierPrice() : BigDecimal.ZERO;
                            BigDecimal laba = hargaJual.subtract(hargaBeli).multiply(BigDecimal.valueOf(qty));

                            return Map.entry(period, new LaporanPerWaktuDTO(period, totalPrice, laba));
                        })
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, incoming) -> new LaporanPerWaktuDTO(
                                existing.getPeriod(),
                                existing.getTotalHargaPenjualan().add(incoming.getTotalHargaPenjualan()),
                                existing.getLabaPenjualan().add(incoming.getLabaPenjualan())
                        )
                ));

        // Ensure all periods are present in the final map, even if zero
        Map<String, LaporanPerWaktuDTO> finalMap = new TreeMap<>(); // TreeMap to ensure natural ordering
        for (String period : allPeriods) {
            LaporanPerWaktuDTO data = groupedMap.getOrDefault(
                    period,
                    new LaporanPerWaktuDTO(period, BigDecimal.ZERO, BigDecimal.ZERO)
            );
            finalMap.put(period, data);
        }

        return new ArrayList<>(finalMap.values());
    }

    public List<LaporanPerPelangganDTO> getLaporanPenjualanDataByCustomer(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<TransactionEntity> transactionData;

        transactionData = transactionRepository
                .findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(
                        clientId, startDate, endDate);


        // Preload all products (assuming shortName is unique)
        Map<String, ProductEntity> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(ProductEntity::getShortName, Function.identity(), (a, b) -> a));

        // Group by customer name
        Map<String, LaporanPerPelangganDTO> groupedByCustomer = transactionData.stream()
                .flatMap(trx -> trx.getTransactionDetailEntities().stream()
                        .filter(detail -> detail.getDeletedAt() == null)
                        .map(detail -> {
                            String customerName = trx.getCustomerEntity() != null
                                    ? trx.getCustomerEntity().getName()
                                    : "Unknown Customer";

                            BigDecimal hargaJual = detail.getPrice();
                            BigDecimal totalPrice = detail.getTotalPrice();
                            Long qty = detail.getQty();

                            ProductEntity product = productMap.get(detail.getShortName());
                            BigDecimal hargaBeli = (product != null) ? product.getSupplierPrice() : BigDecimal.ZERO;
                            BigDecimal laba = hargaJual.subtract(hargaBeli).multiply(BigDecimal.valueOf(qty));

                            return Map.entry(customerName,
                                    new LaporanPerPelangganDTO(customerName, totalPrice, laba));
                        })
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, incoming) -> new LaporanPerPelangganDTO(
                                existing.getCustomerName(),
                                existing.getTotalHargaPenjualan().add(incoming.getTotalHargaPenjualan()),
                                existing.getLabaPenjualan().add(incoming.getLabaPenjualan())
                        )
                ));

        return groupedByCustomer.values().stream()
                .sorted(Comparator.comparing(LaporanPerPelangganDTO::getCustomerName))
                .collect(Collectors.toList());
    }


    private List<String> generateAllPeriods(LocalDate startDate, LocalDate endDate, String filterOptions) {
        List<String> periods = new ArrayList<>();
        DateTimeFormatter formatter;

        switch (filterOptions != null ? filterOptions.toLowerCase() : "") {
            case "year":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                Year startYear = Year.from(startDate);
                Year endYear = Year.from(endDate);
                for (Year year = startYear; !year.isAfter(endYear); year = year.plusYears(1)) {
                    periods.add(year.toString());
                }
                break;

            case "month":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                YearMonth startMonth = YearMonth.from(startDate);
                YearMonth endMonth = YearMonth.from(endDate);
                for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
                    periods.add(ym.format(formatter));
                }
                break;

            default: // day
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    periods.add(date.format(formatter));
                }
                break;
        }

        return periods;
    }


    public List<PenjualanDTO> getLast10Transaction(Long clientId) {
        LocalDateTime startDate = LocalDate.now().atStartOfDay();
        LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);
//        For testing only
//        LocalDateTime startDate = LocalDate.parse("2025-09-13").atStartOfDay();
//        LocalDateTime endDate = LocalDate.parse("2025-09-13").atTime(23, 59, 59);
        List<TransactionEntity> transactionData = transactionRepository.findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate).stream().limit(10).toList();
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
                                transactionDetail.getTotalPrice()
                        ))
                        .collect(Collectors.toList())
        )).collect(Collectors.toList());
    }

    public List<PenjualanDTO> getPenjualanData(Long clientId, LocalDateTime startDate, LocalDateTime endDate, Long customerId) {
        List<TransactionEntity> transactionData;
        if (customerId == null) {
            transactionData = transactionRepository.findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate);
        } else {
            transactionData = transactionRepository.findAllByClientEntity_ClientIdAndCustomerEntity_CustomerIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(clientId, customerId, startDate, endDate);
        }
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
                                transactionDetail.getTotalPrice()
                        ))
                        .collect(Collectors.toList())  // collect the stream to a list
        )).collect(Collectors.toList());
    }

    public PenjualanDTO getPenjualanDataById(Long clientId, Long penjualanId) {
        Optional<TransactionEntity> transactionsOpt = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNull(clientId, penjualanId);
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
                                transactionDetail.getTotalPrice()
                        ))
                        .collect(Collectors.toList())
        );
    }

    @Transactional
    public boolean deletePenjualan(Long transactionId, ClientEntity clientData) {
        Optional<TransactionEntity> transactionEntityOpt = transactionRepository.findFirstByClientEntity_ClientIdAndTransactionIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNull(clientData.getClientId(), transactionId);
        if (transactionEntityOpt.isEmpty()) {
            System.out.println("Transaction not found");
            return false;
        }
        TransactionEntity transactionEntity = transactionEntityOpt.get();


        //Restore stock from old transaction
        List<TransactionDetailEntity> oldTransactions = transactionDetailRepository.findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(transactionId);
        for (TransactionDetailEntity old : oldTransactions) {
            ProductEntity product = productRepository.findFirstByFullNameOrShortNameAndDeletedAtIsNullAndClientEntity_ClientId(old.getFullName(), old.getShortName(), clientData.getClientId());
            if (product != null) {
                Long restoredStock = product.getStock() + old.getQty();
                product.setStock(restoredStock);
                productRepository.save(product);

                boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                        product,
                        transactionEntity.getTransactionNumber(),
                        TipeKartuStok.KOREKSI_PENJUALAN,
                        old.getQty(),
                        0L,
                        restoredStock,
                        clientData
                ));
                if (!isAdjusted) {
                    System.out.println("Gagal adjust di delete penjualan");
                    return false;
                }
            }
            old.setDeletedAt(getCurrentTimestamp());
            transactionDetailRepository.save(old);
        }

        transactionEntity.setDeletedAt(getCurrentTimestamp());
        transactionRepository.save(transactionEntity);

        return true;
    }
}
