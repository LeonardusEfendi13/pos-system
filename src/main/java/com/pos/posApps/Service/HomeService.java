package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.Home.ChartDTO;
import com.pos.posApps.DTO.Dtos.Home.HomeCustomerDTO;
import com.pos.posApps.DTO.Dtos.Home.HomeProductDTO;
import com.pos.posApps.DTO.Dtos.Home.HomeTopBarDTO;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HomeService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    @Autowired
    private PurchasingRepository purchasingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    public HomeTopBarDTO getHomeTopBarData(Long clientId) {
        LocalDateTime startDate = LocalDate.now().atStartOfDay();
        LocalDateTime endDate = LocalDate.now().atTime(23, 59, 59);

        //Fetch all transaction Data by todays date
        List<TransactionEntity> transactionData = transactionRepository.findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate);

        //Count transaction data
        Long transactionCount = (long) transactionData.size();

        //Sum transaction total price
        BigDecimal totalTransaction = transactionData.stream().map(TransactionEntity::getTotalPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        //Get total profit
        //Step 1. Load all product
        List<ProductEntity> allProducts = productRepository.findAllByClientEntity_ClientIdAndProductPricesEntityIsNotNullAndDeletedAtIsNullOrderByProductIdDesc(clientId);
        Map<String, ProductEntity> productMap = allProducts.stream().collect(Collectors.toMap(ProductEntity::getShortName, Function.identity()));

        //Step 2. Initiate total profit
        BigDecimal totalProfit = BigDecimal.ZERO;

        //Step 3. Calculate total profit
        for (TransactionEntity transactions : transactionData) {
            for (TransactionDetailEntity details : transactions.getTransactionDetailEntities()) {
                ProductEntity product = productMap.get(details.getShortName());
                if (product != null && details.getPrice() != null && product.getSupplierPrice() != null) {
                    BigDecimal priceDiff = details.getPrice().subtract(product.getSupplierPrice());
                    BigDecimal profit = priceDiff.multiply(BigDecimal.valueOf(details.getQty()));
                    totalProfit = totalProfit.add(profit);
                }
            }
        }

        return new HomeTopBarDTO(transactionCount, totalTransaction, totalProfit);
    }

    public List<HomeProductDTO> getTop10Product(LocalDateTime startDate, LocalDateTime endDate) {
        //Fetch all the data within the dates
        List<TransactionDetailEntity> transactionDetailEntities = transactionDetailRepository.findAllByDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionDetailIdDesc(startDate, endDate);
        //Initiate Map to store data
        Map<String, Long> productMap = new HashMap<>();

        for (TransactionDetailEntity data : transactionDetailEntities) {
            String shortName = data.getShortName();
            Long qty = data.getQty() != null ? data.getQty() : 0L;
            productMap.put(shortName, productMap.getOrDefault(shortName, 0L) + qty);
        }

        //Load all product data (you can optimize this later)
        List<ProductEntity> allProducts = productRepository.findAll();
        Map<String, String> shortNameToFullName = allProducts.stream()
                .collect(Collectors.toMap(ProductEntity::getShortName, ProductEntity::getFullName));


        //Sort by qty descending and get top 10
        return productMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(entry -> {
                    String shortName = entry.getKey();
                    Long qty = entry.getValue();
                    String fullName = shortNameToFullName.getOrDefault(shortName, shortName); // fallback to shortName
                    return new HomeProductDTO(fullName, qty);
                })
                .collect(Collectors.toList());
    }

    public List<HomeCustomerDTO> getTop5Customer(Long clientId, LocalDateTime startDate, LocalDateTime endDate) {
        //Fetch all transactions
        List<TransactionEntity> transactionData = transactionRepository.findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate);

        //Populate totalPrice using customer's Id
        Map<Long, BigDecimal> firstCustomerMap = new HashMap<>();
        for (TransactionEntity data : transactionData) {
            Long customerId = data.getCustomerEntity().getCustomerId();
            BigDecimal totalPrice = data.getTotalPrice() != null ? data.getTotalPrice() : BigDecimal.ZERO;
            firstCustomerMap.put(customerId, firstCustomerMap.getOrDefault(customerId, BigDecimal.ZERO).add(totalPrice));
        }

        //Sort and limit to 5
        List<Long> top5 = firstCustomerMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        //Fetch all customer data
        List<CustomerEntity> customerEntities = customerRepository.findAllByClientEntity_ClientIdOrderByCustomerIdDesc(clientId);

        //Map Customer name with customerId
        Map<Long, String> idToNameMap = customerEntities.stream().collect(Collectors.toMap(CustomerEntity::getCustomerId, CustomerEntity::getName));

        return top5.stream().map(id -> new HomeCustomerDTO(idToNameMap.getOrDefault(id, "Unknown"), firstCustomerMap.get(id))).toList();
    }

    public ChartDTO getChartData(Long clientId, LocalDateTime adjustedStartDate, LocalDateTime adjustedEndDate, String periodFilter) {
        // Adjust range based on periodFilter
//        LocalDateTime adjustedStartDate = adjustStartDate(startDate, periodFilter);
//        LocalDateTime adjustedEndDate = adjustEndDate(endDate, periodFilter);

        List<String> labels = generateLabels(adjustedStartDate, adjustedEndDate, periodFilter);

        Map<String, BigDecimal> pendapatanMap = getPendapatan(clientId, adjustedStartDate, adjustedEndDate, periodFilter);
        Map<String, BigDecimal> pengeluaranMap = getPengeluaran(clientId, adjustedStartDate, adjustedEndDate, periodFilter);
        Map<String, BigDecimal> labaMap = getLaba(clientId, adjustedStartDate, adjustedEndDate, periodFilter);

        List<BigDecimal> pendapatan = labels.stream()
                .map(label -> pendapatanMap.getOrDefault(label, BigDecimal.ZERO))
                .collect(Collectors.toList());

        List<BigDecimal> pengeluaran = labels.stream()
                .map(label -> pengeluaranMap.getOrDefault(label, BigDecimal.ZERO))
                .collect(Collectors.toList());

        List<BigDecimal> laba = labels.stream()
                .map(label -> labaMap.getOrDefault(label, BigDecimal.ZERO))
                .collect(Collectors.toList());

        ChartDTO chartDTO = new ChartDTO();
        chartDTO.setLabels(labels);
        chartDTO.setPendapatan(pendapatan);
        chartDTO.setPengeluaran(pengeluaran);
        chartDTO.setLaba(laba);

        return chartDTO;
    }

    // Adjust start date based on period
    public LocalDateTime adjustStartDate(LocalDateTime start, String periodFilter) {
        switch (periodFilter.toLowerCase()) {
            case "year":
                // pindahkan ke 1 Januari tahun start
                return LocalDateTime.of(start.getYear(), 1, 1, 0, 0, 0);
            case "month":
                // pindahkan ke 1 hari di bulan start (full month)
                // Tambahan: sesuai permintaan, extend ke 1 bulan sebelum start untuk cover bulan penuh
                return LocalDateTime.of(start.getYear(), start.getMonth(), 1, 0, 0, 0);
            case "day":
            default:
                // biarkan apa adanya (per hari)
                return start.withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
    }

    // Adjust end date based on period
    public LocalDateTime adjustEndDate(LocalDateTime end, String periodFilter) {
        switch (periodFilter.toLowerCase()) {
            case "year":
                // pindahkan ke 31 Desember tahun end jam 23:59:59.999
                return LocalDateTime.of(end.getYear(), 12, 31, 23, 59, 59, 999_999_999);
            case "month":
                // pindahkan ke akhir bulan dari end (full month)
                // Untuk memperjelas, end bulan itu tanggal terakhir bulan tersebut
                LocalDateTime endMonthLastDay = end.withDayOfMonth(end.toLocalDate().lengthOfMonth())
                        .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
                return endMonthLastDay;
            case "day":
            default:
                // biarkan apa adanya, tapi set jam akhir hari
                return end.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        }
    }

    private List<String> generateLabels(LocalDateTime start, LocalDateTime end, String periodFilter) {
        List<String> labels = new ArrayList<>();

        DateTimeFormatter formatter;
        ChronoUnit stepUnit;

        switch (periodFilter.toLowerCase()) {
            case "year":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                stepUnit = ChronoUnit.YEARS;
                break;
            case "month":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                stepUnit = ChronoUnit.MONTHS;
                break;
            case "day":
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                stepUnit = ChronoUnit.DAYS;
                break;
        }

        LocalDateTime current = start;
        while (!current.isAfter(end)) {
            labels.add(current.format(formatter));
            current = current.plus(1, stepUnit);
        }

        return labels;
    }

    private String formatDate(LocalDateTime date, String periodFilter) {
        switch (periodFilter.toLowerCase()) {
            case "year":
                return date.format(DateTimeFormatter.ofPattern("yyyy"));
            case "month":
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "day":
            default:
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }

    private Map<String, BigDecimal> getPendapatan(Long clientId, LocalDateTime start, LocalDateTime end, String periodFilter) {
        List<TransactionEntity> transactions = transactionRepository.findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, start, end);

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> formatDate(t.getCreatedAt(), periodFilter),
                        Collectors.reducing(BigDecimal.ZERO, TransactionEntity::getTotalPrice, BigDecimal::add)
                ));
    }

    private Map<String, BigDecimal> getPengeluaran(Long clientId, LocalDateTime start, LocalDateTime end, String periodFilter) {
        List<PurchasingEntity> purchases = purchasingRepository.findAllByClientEntity_ClientIdAndPurchasingDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByPurchasingIdDesc(clientId, start, end);

        return purchases.stream()
                .collect(Collectors.groupingBy(
                        p -> formatDate(p.getCreatedAt(), periodFilter),
                        Collectors.reducing(BigDecimal.ZERO, PurchasingEntity::getTotalPrice, BigDecimal::add)
                ));
    }

    private Map<String, BigDecimal> getLaba(Long clientId, LocalDateTime start, LocalDateTime end, String periodFilter) {
        // Map shortName to supplierPrice
        Map<String, BigDecimal> supplierPrices = productRepository.findAll().stream()
                .collect(Collectors.toMap(ProductEntity::getShortName, ProductEntity::getSupplierPrice));

        List<TransactionEntity> transactions = transactionRepository.findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, start, end);

        Map<String, BigDecimal> labaMap = new HashMap<>();

        for (TransactionEntity transaction : transactions) {
            String label = formatDate(transaction.getCreatedAt(), periodFilter);
            BigDecimal totalProfit = BigDecimal.ZERO;

            for (TransactionDetailEntity detail : transaction.getTransactionDetailEntities()) {
                String shortName = detail.getShortName();
                Long qty = detail.getQty();

                BigDecimal soldPrice = detail.getPrice(); // from TransactionDetail
                BigDecimal costPrice = supplierPrices.getOrDefault(shortName, BigDecimal.ZERO);

                BigDecimal profitPerUnit = soldPrice.subtract(costPrice);
                BigDecimal totalDetailProfit = profitPerUnit.multiply(BigDecimal.valueOf(qty));

                totalProfit = totalProfit.add(totalDetailProfit);
            }

            labaMap.merge(label, totalProfit, BigDecimal::add);
        }

        return labaMap;
    }


}
