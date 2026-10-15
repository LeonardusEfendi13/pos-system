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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
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

    public List<PayrollRowDTO> preview(Long clientId, YearMonth month) {
        Map<Long, Map<LocalDate, AttendanceStatus>> marks = attendanceService.marksFor(clientId, month);
        List<PayrollRowDTO> rows = new ArrayList<>();
        for (StaffEntity staff : attendanceService.employed(clientId, month)) {
            rows.add(calculate(staff, month, marks.getOrDefault(staff.getStaffId(), Map.of())));
        }
        return rows;
    }

    public List<PayrollRowDTO> saved(Long clientId, YearMonth month) {
        return payrollRepository
                .findByClientEntity_ClientIdAndPayrollYearAndPayrollMonthAndDeletedAtIsNull(
                        clientId,
                        month.getYear(),
                        month.getMonthValue()
                )
                .stream()
                .map(this::fromEntity)
                .sorted(Comparator.comparing(row -> row.getNama() == null ? "" : row.getNama(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public List<PayrollRowDTO> save(ClientEntity client, YearMonth month) {
        Long clientId = client.getClientId();
        List<StaffEntity> staffRows = attendanceService.employed(clientId, month);
        if (staffRows.isEmpty()) {
            throw new StaffRuleException("Tidak ada karyawan pada bulan ini");
        }

        Map<Long, Map<LocalDate, AttendanceStatus>> marks = attendanceService.marksFor(clientId, month);
        List<PayrollEntity> liveRows = payrollRepository
                .findByClientEntity_ClientIdAndPayrollYearAndPayrollMonthAndDeletedAtIsNull(
                        clientId,
                        month.getYear(),
                        month.getMonthValue()
                );
        Map<Long, PayrollEntity> byStaff = new HashMap<>();
        for (PayrollEntity row : liveRows) {
            if (row.getStaffEntity() != null && row.getStaffEntity().getStaffId() != null) {
                byStaff.put(row.getStaffEntity().getStaffId(), row);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<PayrollRowDTO> calculated = new ArrayList<>();
        for (StaffEntity staff : staffRows) {
            PayrollRowDTO row = calculate(staff, month, marks.getOrDefault(staff.getStaffId(), Map.of()));
            PayrollEntity entity = byStaff.remove(staff.getStaffId());
            if (entity == null) {
                entity = new PayrollEntity();
                entity.setClientEntity(client);
                entity.setStaffEntity(staff);
                entity.setPayrollMonth(month.getMonthValue());
                entity.setPayrollYear(month.getYear());
            }
            entity.setStaffName(row.getNama());
            entity.setDaysInMonth(row.getDaysInMonth());
            entity.setAbsenceCount(row.getAbsenceCount());
            entity.setDeductedDays(row.getDeductedDays());
            entity.setDailyRate(row.getDailyRate());
            entity.setGajiPokok(row.getGajiPokok());
            entity.setUangMakan(row.getUangMakan());
            entity.setInsentif(row.getInsentif());
            entity.setPotongan(row.getPotongan());
            entity.setGajiPokokDibayar(row.getGajiPokokDibayar());
            entity.setTotalPayroll(row.getTotal());
            entity.setDeletedAt(null);
            payrollRepository.save(entity);
            calculated.add(row);
        }
        for (PayrollEntity leftover : byStaff.values()) {
            leftover.setDeletedAt(now);
            payrollRepository.save(leftover);
        }
        return calculated;
    }

    private PayrollRowDTO calculate(
            StaffEntity staff,
            YearMonth month,
            Map<LocalDate, AttendanceStatus> marks
    ) {
        int absences = PayrollCalculator.countAbsences(
                month,
                staff.getTanggalJoin(),
                staff.getTanggalResign(),
                marks
        );
        PayrollCalculator.Result result = PayrollCalculator.calculate(new PayrollCalculator.Input(
                staff.getGajiPokok(),
                staff.getUangMakan(),
                staff.getInsentif(),
                month.lengthOfMonth(),
                absences
        ));
        return new PayrollRowDTO(
                staff.getStaffId(),
                staff.getNama(),
                staff.getJabatan() == null ? "" : staff.getJabatan().name(),
                result.gajiPokok(),
                result.uangMakan(),
                result.insentif(),
                result.daysInMonth(),
                result.absenceCount(),
                result.deductedDays(),
                result.dailyRate(),
                result.potongan(),
                result.gajiPokokDibayar(),
                result.total()
        );
    }

    private PayrollRowDTO fromEntity(PayrollEntity entity) {
        StaffEntity staff = entity.getStaffEntity();
        Long staffId = staff == null ? null : staff.getStaffId();
        String jabatan = staff == null || staff.getJabatan() == null ? "" : staff.getJabatan().name();
        return new PayrollRowDTO(
                staffId,
                entity.getStaffName(),
                jabatan,
                entity.getGajiPokok(),
                entity.getUangMakan(),
                entity.getInsentif(),
                entity.getDaysInMonth(),
                entity.getAbsenceCount(),
                entity.getDeductedDays(),
                entity.getDailyRate(),
                entity.getPotongan(),
                entity.getGajiPokokDibayar(),
                entity.getTotalPayroll()
        );
    }
}
