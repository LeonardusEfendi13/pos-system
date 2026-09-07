package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.Home.DashboardHomeSupplierDTO;
import com.pos.posApps.DTO.Dtos.Home.SupplierRankingView;
import com.pos.posApps.Repository.DashboardSupplierRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class DashboardSupplierService {
    public static final int TOP_SUPPLIER_LIMIT = 5;

    private DashboardSupplierRepository dashboardSupplierRepository;

    public List<DashboardHomeSupplierDTO> getTopSuppliers(
            Long clientId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return dashboardSupplierRepository
                .findTopSuppliers(clientId, startDate, endDate, TOP_SUPPLIER_LIMIT)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private DashboardHomeSupplierDTO toDto(SupplierRankingView row) {
        String name = row.getName() == null ? "" : row.getName();
        Long invoiceCount = row.getInvoiceCount() == null ? 0L : row.getInvoiceCount();
        BigDecimal totalSpending = row.getTotalSpending() == null ? BigDecimal.ZERO : row.getTotalSpending();
        BigDecimal unpaidTotal = row.getUnpaidTotal() == null ? BigDecimal.ZERO : row.getUnpaidTotal();

        return new DashboardHomeSupplierDTO(name, invoiceCount, totalSpending, unpaidTotal);
    }
}
