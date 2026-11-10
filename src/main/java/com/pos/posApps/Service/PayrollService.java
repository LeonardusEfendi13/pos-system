package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.PayrollRowDTO;
import com.pos.posApps.DTO.Enum.AttendanceStatus;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.PayrollEntity;
import com.pos.posApps.Entity.StaffEntity;
import com.pos.posApps.Repository.PayrollRepository;
import com.pos.posApps.Util.PayrollCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PayrollService {
    private final PayrollRepository payrollRepository;
    private final StaffAttendanceService attendanceService;

    public PayrollService(PayrollRepository payrollRepository, StaffAttendanceService attendanceService) {
        this.payrollRepository = payrollRepository;
        this.attendanceService = attendanceService;
    }

    public YearMonth window(Integer month, Integer year) {
        if (month == null || year == null || month < 1 || month > 12 || year < 2000 || year > 2100) {
            throw new StaffRuleException("Bulan atau tahun tidak valid");
        }
        return YearMonth.of(year, month);
    }

    /**
     * Calculated live from current staff and attendance, up to today. A month already
     * marked paid shows the amounts saved when it was paid, so later changes to a
     * staff's pay or insentif do not rewrite it.
     */
    public List<PayrollRowDTO> rows(Long clientId, YearMonth month, LocalDate today) {
        Map<Long, Map<LocalDate, AttendanceStatus>> marks = attendanceService.marksFor(clientId, month);
        Map<Long, PayrollEntity> paid = new HashMap<>();
        for (PayrollEntity entity : liveRows(clientId, month)) {
            if (entity.getStaffEntity() != null && entity.getPaidAt() != null) {
                paid.put(entity.getStaffEntity().getStaffId(), entity);
            }
        }

        List<PayrollRowDTO> rows = new ArrayList<>();
        for (StaffEntity staff : attendanceService.employed(clientId, month)) {
            PayrollCalculator.Result result = calculate(
                    staff,
                    month,
                    today,
                    marks.getOrDefault(staff.getStaffId(), Map.of())
            );
            PayrollEntity saved = paid.get(staff.getStaffId());
            if (saved != null) {
                result = withSavedAmounts(result, saved);
            }
            rows.add(toRow(staff, result, saved == null ? null : saved.getPaidAt()));
        }
        return rows;
    }

    /**
     * Swaps the live amounts for the snapshot saved at payment. Worked and present days
     * are not saved, so they stay live. A legacy row without a saved total stays live.
     */
    private PayrollCalculator.Result withSavedAmounts(PayrollCalculator.Result live, PayrollEntity saved) {
        if (saved.getTotalPayroll() == null) {
            return live;
        }

        BigDecimal pokok = savedOr(saved.getGajiPokok(), live.gajiPokok());
        BigDecimal makan = savedOr(saved.getUangMakan(), live.uangMakan());
        BigDecimal bonus = savedOr(saved.getBonus(), live.bonus());
        return new PayrollCalculator.Result(
                saved.getDaysInMonth() > 0 ? saved.getDaysInMonth() : live.daysInMonth(),
                live.workedDays(),
                live.presentCount(),
                saved.getAbsenceCount(),
                saved.getDeductedDays(),
                pokok,
                makan,
                bonus,
                savedOr(saved.getInsentif(), live.insentif()),
                pokok.add(makan).add(bonus),
                savedOr(saved.getDailyRate(), live.dailyRate()),
                savedOr(saved.getGajiPokokDibayar(), live.gajiTerkumpul()),
                savedOr(saved.getPotongan(), live.potongan()),
                saved.getTotalPayroll()
        );
    }

    private BigDecimal savedOr(BigDecimal saved, BigDecimal live) {
        return saved == null ? live : saved;
    }

    @Transactional
    public void markPaid(ClientEntity client, Long staffId, YearMonth month, LocalDate today) {
        Long clientId = client.getClientId();
        StaffEntity staff = employedStaff(clientId, staffId, month);
        PayrollEntity entity = payrollRepository
                .findFirstByClientEntity_ClientIdAndStaffEntity_StaffIdAndPayrollYearAndPayrollMonthAndDeletedAtIsNull(
                        clientId,
                        staffId,
                        month.getYear(),
                        month.getMonthValue()
                )
                .orElse(null);
        if (entity != null && entity.getPaidAt() != null) {
            throw new StaffRuleException("Gaji " + staff.getNama() + " sudah ditandai dibayar");
        }
        if (entity == null) {
            entity = new PayrollEntity();
            entity.setClientEntity(client);
            entity.setStaffEntity(staff);
            entity.setPayrollMonth(month.getMonthValue());
            entity.setPayrollYear(month.getYear());
        }

        Map<LocalDate, AttendanceStatus> marks = attendanceService
                .marksFor(clientId, month)
                .getOrDefault(staffId, Map.of());
        PayrollCalculator.Result result = calculate(staff, month, today, marks);
        entity.setStaffName(staff.getNama());
        entity.setDaysInMonth(result.daysInMonth());
        entity.setAbsenceCount(result.absenceCount());
        entity.setDeductedDays(result.deductedDays());
        entity.setDailyRate(result.dailyRate());
        entity.setGajiPokok(result.gajiPokok());
        entity.setUangMakan(result.uangMakan());
        entity.setBonus(result.bonus());
        entity.setInsentif(result.insentif());
        entity.setPotongan(result.potongan());
        // Legacy column: now holds the earned total before deductions
        entity.setGajiPokokDibayar(result.gajiTerkumpul());
        entity.setTotalPayroll(result.total());
        entity.setPaidAt(LocalDateTime.now());
        payrollRepository.save(entity);
    }

    @Transactional
    public void cancelPaid(Long clientId, Long staffId, YearMonth month) {
        PayrollEntity entity = payrollRepository
                .findFirstByClientEntity_ClientIdAndStaffEntity_StaffIdAndPayrollYearAndPayrollMonthAndDeletedAtIsNull(
                        clientId,
                        staffId,
                        month.getYear(),
                        month.getMonthValue()
                )
                .orElse(null);
        if (entity == null || entity.getPaidAt() == null) {
            throw new StaffRuleException("Gaji karyawan ini belum ditandai dibayar");
        }
        if (entity.getInsentifPayoutId() != null) {
            throw new StaffRuleException("Insentif bulan ini sudah dicairkan, pembayaran gaji tidak bisa dibatalkan");
        }
        entity.setPaidAt(null);
        payrollRepository.save(entity);
    }

    private List<PayrollEntity> liveRows(Long clientId, YearMonth month) {
        return payrollRepository.findByClientEntity_ClientIdAndPayrollYearAndPayrollMonthAndDeletedAtIsNull(
                clientId,
                month.getYear(),
                month.getMonthValue()
        );
    }

    private StaffEntity employedStaff(Long clientId, Long staffId, YearMonth month) {
        if (staffId == null) {
            throw new StaffRuleException("Karyawan tidak ditemukan", true);
        }
        return attendanceService.employed(clientId, month).stream()
                .filter(staff -> staffId.equals(staff.getStaffId()))
                .findFirst()
                .orElseThrow(() -> new StaffRuleException("Karyawan tidak bekerja pada bulan ini", true));
    }

    private PayrollCalculator.Result calculate(
            StaffEntity staff,
            YearMonth month,
            LocalDate today,
            Map<LocalDate, AttendanceStatus> marks
    ) {
        int worked = PayrollCalculator.workedDays(
                month,
                staff.getTanggalJoin(),
                staff.getTanggalResign(),
                today
        );
        int present = PayrollCalculator.countPresent(
                month,
                staff.getTanggalJoin(),
                staff.getTanggalResign(),
                today,
                marks
        );
        int absences = PayrollCalculator.countAbsences(
                month,
                staff.getTanggalJoin(),
                staff.getTanggalResign(),
                today,
                marks
        );
        return PayrollCalculator.calculate(new PayrollCalculator.Input(
                staff.getGajiPokok(),
                staff.getUangMakan(),
                staff.getBonus(),
                staff.getInsentif(),
                month.lengthOfMonth(),
                worked,
                present,
                absences
        ));
    }

    private PayrollRowDTO toRow(StaffEntity staff, PayrollCalculator.Result result, LocalDateTime paidAt) {
        return new PayrollRowDTO(
                staff.getStaffId(),
                staff.getNama(),
                staff.getJabatan() == null ? "" : staff.getJabatan().name(),
                result.gajiPokok(),
                result.uangMakan(),
                result.bonus(),
                result.insentif(),
                result.totalGaji(),
                result.daysInMonth(),
                result.workedDays(),
                result.presentCount(),
                result.absenceCount(),
                result.deductedDays(),
                result.dailyRate(),
                result.gajiTerkumpul(),
                result.potongan(),
                result.total(),
                paidAt
        );
    }
}
