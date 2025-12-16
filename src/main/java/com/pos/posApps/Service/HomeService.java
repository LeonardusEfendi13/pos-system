package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.ChartPointDTO;
import com.pos.posApps.DTO.Dtos.Home.ChartDTO;
import com.pos.posApps.DTO.Dtos.Home.HomeCustomerDTO;
import com.pos.posApps.DTO.Dtos.Home.HomeProductDTO;
import com.pos.posApps.DTO.Dtos.Home.HomeTopBarDTO;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

        // Fetch all transaction Data by todays date
        List<TransactionEntity> transactionData =
                transactionRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(
                        clientId, startDate, endDate
                );


        // Count transaction data
        Long transactionCount = (long) transactionData.size();

        // Sum transaction total price
        BigDecimal totalTransaction = transactionData.stream()
                .map(TransactionEntity::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sum Total profit
        BigDecimal totalProfit = transactionData.stream()
                .flatMap(t -> t.getTransactionDetailEntities().stream())
                .map(TransactionDetailEntity::getTotalProfit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        return new HomeTopBarDTO(transactionCount, totalTransaction, totalProfit);
    }
    public List<HomeProductDTO> getTop10Product(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionDetailRepository.findTopProducts(
                startDate,
                endDate,
                PageRequest.of(0, 10)
        );
    }

    public List<HomeCustomerDTO> getTop5Customer(Long clientId, LocalDateTime startDate, LocalDateTime endDate) {
        //Fetch all transactions
        List<TransactionEntity> transactionData = transactionRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate);

        //Populate totalPrice using customer's Id
        Map<Long, BigDecimal> firstCustomerMap = new HashMap<>();
        for (TransactionEntity data : transactionData) {
            Long customerId = data.getCustomerEntity().getCustomerId();
            BigDecimal totalPrice = data.getTotalPrice() != null ? data.getTotalPrice() : BigDecimal.ZERO;
            firstCustomerMap.put(customerId, firstCustomerMap.getOrDefault(customerId, BigDecimal.ZERO).add(totalPrice));
        }

        //Sort and limit to 5
        List<Long> top5 = firstCustomerMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();

        //Fetch all customer data
        List<CustomerEntity> customerEntities = customerRepository.findAllByClientEntity_ClientIdOrderByCustomerIdDesc(clientId);

        //Map Customer name with customerId
        Map<Long, String> idToNameMap = customerEntities.stream().collect(Collectors.toMap(CustomerEntity::getCustomerId, CustomerEntity::getName));

        return top5.stream().map(id -> new HomeCustomerDTO(idToNameMap.getOrDefault(id, "Unknown"), firstCustomerMap.get(id))).toList();
    }

    public ChartDTO getChartData(Long clientId, LocalDateTime start, LocalDateTime end, String periodFilter) {

        String dateFormat = resolveDateFormat(periodFilter);

        List<String> labels = generateLabels(start, end, periodFilter);

        List<ChartPointDTO> pendapatan = transactionRepository.getPendapatanChart(clientId, start, end, dateFormat)
                .stream()
                .map(p -> new ChartPointDTO(p.getLabel(), p.getValue()))
                .toList();
        List<ChartPointDTO> pengeluaran = purchasingRepository.getPengeluaranChart(clientId, start, end, dateFormat)
                .stream()
                .map(p -> new ChartPointDTO(p.getLabel(), p.getValue()))
                .toList();
        List<ChartPointDTO> laba = transactionRepository.getLabaChart(clientId, start, end, dateFormat)
                .stream()
                .map(p -> new ChartPointDTO(p.getLabel(), p.getValue()))
                .toList();
        // Map label -> value for missing labels
        Map<String, BigDecimal> pendapatanMap = pendapatan.stream().collect(Collectors.toMap(ChartPointDTO::getLabel, ChartPointDTO::getValue));
        Map<String, BigDecimal> pengeluaranMap = pengeluaran.stream().collect(Collectors.toMap(ChartPointDTO::getLabel, ChartPointDTO::getValue));
        Map<String, BigDecimal> labaMap = laba.stream().collect(Collectors.toMap(ChartPointDTO::getLabel, ChartPointDTO::getValue));

        ChartDTO chartDTO = new ChartDTO();
        chartDTO.setLabels(labels);
        chartDTO.setPendapatan(labels.stream().map(l -> pendapatanMap.getOrDefault(l, BigDecimal.ZERO)).collect(Collectors.toList()));
        chartDTO.setPengeluaran(labels.stream().map(l -> pengeluaranMap.getOrDefault(l, BigDecimal.ZERO)).collect(Collectors.toList()));
        chartDTO.setLaba(labels.stream().map(l -> labaMap.getOrDefault(l, BigDecimal.ZERO)).collect(Collectors.toList()));

        return chartDTO;
    }

    private String resolveDateFormat(String periodFilter) {
        return switch (periodFilter.toUpperCase()) {
            case "DAY" -> "YYYY-MM-DD";     // e.g., 2025-12-16
            case "MONTH" -> "YYYY-MM";      // e.g., 2025-12
            case "YEAR" -> "YYYY";          // e.g., 2025
            default -> throw new IllegalArgumentException("Invalid period filter: " + periodFilter);
        };
    }

    private List<String> generateLabels(
            LocalDateTime start,
            LocalDateTime end,
            String periodFilter
    ) {
        List<String> labels = new ArrayList<>();
        DateTimeFormatter formatter;

        switch (periodFilter.toUpperCase()) {
            case "DAY":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (LocalDate date = start.toLocalDate();
                     !date.isAfter(end.toLocalDate());
                     date = date.plusDays(1)) {
                    labels.add(date.format(formatter));
                }
                break;

            case "MONTH":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                for (LocalDate date = start.toLocalDate().withDayOfMonth(1);
                     !date.isAfter(end.toLocalDate());
                     date = date.plusMonths(1)) {
                    labels.add(date.format(formatter));
                }
                break;

            case "YEAR":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                for (int year = start.getYear(); year <= end.getYear(); year++) {
                    labels.add(String.valueOf(year));
                }
                break;
        }
        return labels;
    }

    // Adjust start date based on period
    public LocalDateTime adjustStartDate(LocalDateTime start, String periodFilter) {
        return switch (periodFilter.toLowerCase()) {
            case "year" ->
                // pindahkan ke 1 Januari tahun start
                    LocalDateTime.of(start.getYear(), 1, 1, 0, 0, 0);
            case "month" ->
                // pindahkan ke 1 hari di bulan start (full month)
                // Tambahan: sesuai permintaan, extend ke 1 bulan sebelum start untuk cover bulan penuh
                    LocalDateTime.of(start.getYear(), start.getMonth(), 1, 0, 0, 0);
            default ->
                // biarkan apa adanya (per hari)
                    start.withHour(0).withMinute(0).withSecond(0).withNano(0);
        };
    }

    // Adjust end date based on period
    public LocalDateTime adjustEndDate(LocalDateTime end, String periodFilter) {
        return switch (periodFilter.toLowerCase()) {
            case "year" ->
                // pindahkan ke 31 Desember tahun end jam 23:59:59.999
                    LocalDateTime.of(end.getYear(), 12, 31, 23, 59, 59, 999_999_999);
            case "month" ->
                // pindahkan ke akhir bulan dari end (full month)
                // Untuk memperjelas, end bulan itu tanggal terakhir bulan tersebut
                    end.withDayOfMonth(end.toLocalDate().lengthOfMonth())
                            .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
            default ->
                // biarkan apa adanya, tapi set jam akhir hari
                    end.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        };
    }
}
