package com.pos.posApps.Service;

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

    public HomeTopBarDTO getHomeTopBarData(Long clientId){
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
        for(TransactionEntity transactions : transactionData){
            for(TransactionDetailEntity details: transactions.getTransactionDetailEntities()){
                ProductEntity product = productMap.get(details.getShortName());
                if(product != null && details.getPrice() != null && product.getSupplierPrice() != null){
                    BigDecimal priceDiff = details.getPrice().subtract(product.getSupplierPrice());
                    BigDecimal profit = priceDiff.multiply(BigDecimal.valueOf(details.getQty()));
                    totalProfit = totalProfit.add(profit);
                }
            }
        }

        return new HomeTopBarDTO(transactionCount, totalTransaction, totalProfit);
    }

    public List<HomeProductDTO> getTop10Product(LocalDateTime startDate, LocalDateTime endDate){
        //Fetch all the data within the dates
        List<TransactionDetailEntity> transactionDetailEntities = transactionDetailRepository.findAllByDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionDetailIdDesc(startDate, endDate);
        //Initiate Map to store data
        Map<String, Long> productMap = new HashMap<>();

        for(TransactionDetailEntity data: transactionDetailEntities){
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

    public List<HomeCustomerDTO> getTop5Customer(Long clientId, LocalDateTime startDate, LocalDateTime endDate){
        //Fetch all transactions
        List<TransactionEntity> transactionData = transactionRepository.findAllByClientEntity_ClientIdAndTransactionDetailEntitiesIsNotNullAndDeletedAtIsNullAndCreatedAtBetweenOrderByTransactionIdDesc(clientId, startDate, endDate);

        //Populate totalPrice using customer's Id
        Map<Long, BigDecimal> firstCustomerMap = new HashMap<>();
        for(TransactionEntity data: transactionData){
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
        List<CustomerEntity> customerEntities = customerRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByCustomerIdDesc(clientId);

        //Map Customer name with customerId
        Map<Long, String> idToNameMap = customerEntities.stream().collect(Collectors.toMap(CustomerEntity::getCustomerId, CustomerEntity::getName));

        return top5.stream().map(id -> new HomeCustomerDTO(idToNameMap.getOrDefault(id, "Unknown"), firstCustomerMap.get(id))).toList();


    }


}
