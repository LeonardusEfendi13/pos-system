package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.InsentifRowDTO;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.InsentifPayoutEntity;
import com.pos.posApps.Entity.PayrollEntity;
import com.pos.posApps.Entity.StaffEntity;
import com.pos.posApps.Repository.InsentifPayoutRepository;
import com.pos.posApps.Repository.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Insentif is saved per paid payroll month and cashed out in full. The balance is
 * the insentif snapshot of every paid month not yet linked to a payout.
 */
@Service
public class InsentifService {
    private final PayrollRepository payrollRepository;
    private final InsentifPayoutRepository payoutRepository;

    public InsentifService(PayrollRepository payrollRepository, InsentifPayoutRepository payoutRepository) {
        this.payrollRepository = payrollRepository;
        this.payoutRepository = payoutRepository;
    }

    /** Staff with a non-zero saved balance, by name. */
    public List<InsentifRowDTO> rows(Long clientId) {
        Map<Long, List<PayrollEntity>> savedByStaff = savedByStaff(clientId);
        Map<Long, InsentifPayoutEntity> lastPayout = new HashMap<>();
        for (InsentifPayoutEntity payout : payoutRepository.findByClientEntity_ClientIdAndDeletedAtIsNullOrderByPaidAtDesc(clientId)) {
            if (payout.getStaffEntity() != null) {
                lastPayout.putIfAbsent(payout.getStaffEntity().getStaffId(), payout);
            }
        }

        List<InsentifRowDTO> rows = new ArrayList<>();
        for (List<PayrollEntity> months : savedByStaff.values()) {
            BigDecimal saldo = sum(months);
            if (saldo.signum() == 0) {
                continue;
            }
            PayrollEntity first = months.get(0);
            PayrollEntity last = months.get(months.size() - 1);
            StaffEntity staff = first.getStaffEntity();
            InsentifPayoutEntity payout = lastPayout.get(staff.getStaffId());
            rows.add(new InsentifRowDTO(
                    staff.getStaffId(),
                    staff.getNama() == null ? first.getStaffName() : staff.getNama(),
                    staff.getJabatan() == null ? "" : staff.getJabatan().name(),
                    staff.getTanggalResign() != null,
                    money(staff.getInsentif()),
                    months.size(),
                    period(first),
                    period(last),
                    saldo,
                    payout == null ? null : payout.getPaidAt(),
                    payout == null ? null : payout.getAmount()
            ));
        }
        rows.sort(Comparator.comparing(row -> row.getNama() == null ? "" : row.getNama(), String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    @Transactional
    public BigDecimal cashOut(ClientEntity client, Long staffId) {
        if (staffId == null) {
            throw new StaffRuleException("Karyawan tidak ditemukan", true);
        }
        List<PayrollEntity> months = savedByStaff(client.getClientId()).getOrDefault(staffId, List.of());
        BigDecimal saldo = sum(months);
        if (saldo.signum() == 0) {
            throw new StaffRuleException("Tidak ada insentif yang bisa dicairkan");
        }

        StaffEntity staff = months.get(0).getStaffEntity();
        InsentifPayoutEntity payout = new InsentifPayoutEntity();
        payout.setClientEntity(client);
        payout.setStaffEntity(staff);
        payout.setStaffName(staff.getNama());
        payout.setAmount(saldo);
        payout.setMonthCount(months.size());
        payout.setPaidAt(LocalDateTime.now());
        payoutRepository.save(payout);

        for (PayrollEntity month : months) {
            month.setInsentifPayoutId(payout.getInsentifPayoutId());
        }
        payrollRepository.saveAll(months);
        return saldo;
    }

    private Map<Long, List<PayrollEntity>> savedByStaff(Long clientId) {
        Map<Long, List<PayrollEntity>> byStaff = new LinkedHashMap<>();
        for (PayrollEntity entity : payrollRepository
                .findByClientEntity_ClientIdAndPaidAtIsNotNullAndInsentifPayoutIdIsNullAndDeletedAtIsNullOrderByPayrollYearAscPayrollMonthAsc(clientId)) {
            if (entity.getStaffEntity() == null) {
                continue;
            }
            byStaff.computeIfAbsent(entity.getStaffEntity().getStaffId(), id -> new ArrayList<>()).add(entity);
        }
        return byStaff;
    }

    private BigDecimal sum(List<PayrollEntity> months) {
        return months.stream()
                .map(month -> money(month.getInsentif()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String period(PayrollEntity entity) {
        return String.format("%04d-%02d", entity.getPayrollYear(), entity.getPayrollMonth());
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
