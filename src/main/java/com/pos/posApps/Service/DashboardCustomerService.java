package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.Home.DashboardHomeCustomerDTO;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Entity.TransactionEntity;
import com.pos.posApps.Repository.CustomerRepository;
import com.pos.posApps.Repository.DashboardCustomerRepository;
import com.pos.posApps.Repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DashboardCustomerService {
    private TransactionRepository transactionRepository;
    private DashboardCustomerRepository dashboardCustomerRepository;
    private CustomerRepository customerRepository;

    public List<DashboardHomeCustomerDTO> getTop10CustomerWithProfit(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<TransactionEntity> transactionData =
                transactionRepository.findFilteredTransactions(clientId, startDate, endDate);

        Map<Long, BigDecimal> spendingByCustomer = new HashMap<>();
        for (TransactionEntity data : transactionData) {
            Long customerId = data.getCustomerEntity().getCustomerId();
            BigDecimal totalPrice = data.getTotalPrice() != null ? data.getTotalPrice() : BigDecimal.ZERO;
            spendingByCustomer.put(
                    customerId,
                    spendingByCustomer.getOrDefault(customerId, BigDecimal.ZERO).add(totalPrice)
            );
        }

        Map<Long, BigDecimal> profitByCustomer = new HashMap<>();
        for (Object[] row : dashboardCustomerRepository.sumProfitByCustomer(clientId, startDate, endDate)) {
            if (row[0] == null) {
                continue;
            }

            Long customerId = ((Number) row[0]).longValue();
            BigDecimal profit = row[1] instanceof BigDecimal
                    ? (BigDecimal) row[1]
                    : new BigDecimal(row[1].toString());
            profitByCustomer.put(customerId, profit);
        }

        List<Long> topCustomers = spendingByCustomer.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();

        List<CustomerEntity> customerEntities =
                customerRepository.findAllByClientEntity_ClientIdOrderByCustomerIdDesc(clientId);

        Map<Long, String> idToNameMap = customerEntities.stream()
                .collect(Collectors.toMap(CustomerEntity::getCustomerId, CustomerEntity::getName));

        return topCustomers.stream().map(id -> new DashboardHomeCustomerDTO(
                idToNameMap.getOrDefault(id, "Unknown"),
                spendingByCustomer.get(id),
                profitByCustomer.getOrDefault(id, BigDecimal.ZERO)
        )).toList();
    }
}
