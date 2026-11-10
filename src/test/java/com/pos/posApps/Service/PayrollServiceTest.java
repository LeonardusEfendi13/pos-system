package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.PayrollRowDTO;
import com.pos.posApps.Entity.PayrollEntity;
import com.pos.posApps.Entity.StaffEntity;
import com.pos.posApps.Repository.PayrollRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollServiceTest {
    private static final long CLIENT_ID = 1L;
    private static final YearMonth MONTH = YearMonth.of(2026, 9);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private final PayrollRepository payrollRepository = mock(PayrollRepository.class);
    private final StaffAttendanceService attendanceService = mock(StaffAttendanceService.class);
    private final PayrollService service = new PayrollService(payrollRepository, attendanceService);

    private static BigDecimal rp(long value) {
        return BigDecimal.valueOf(value);
    }

    private StaffEntity staff(long insentif) {
        StaffEntity staff = new StaffEntity();
        staff.setStaffId(7L);
        staff.setNama("Budi");
        staff.setTanggalJoin(LocalDate.of(2026, 1, 1));
        staff.setGajiPokok(rp(1_500_000));
        staff.setUangMakan(rp(300_000));
        staff.setBonus(rp(0));
        staff.setInsentif(rp(insentif));
        return staff;
    }

    private PayrollEntity savedMonth(StaffEntity staff, LocalDateTime paidAt) {
        PayrollEntity entity = new PayrollEntity();
        entity.setStaffEntity(staff);
        entity.setPayrollYear(MONTH.getYear());
        entity.setPayrollMonth(MONTH.getMonthValue());
        entity.setDaysInMonth(30);
        entity.setAbsenceCount(1);
        entity.setDeductedDays(1);
        entity.setDailyRate(rp(60_000));
        entity.setGajiPokok(rp(1_500_000));
        entity.setUangMakan(rp(300_000));
        entity.setBonus(rp(0));
        entity.setInsentif(rp(500_000));
        entity.setPotongan(rp(60_000));
        entity.setGajiPokokDibayar(rp(1_800_000));
        entity.setTotalPayroll(rp(1_740_000));
        entity.setPaidAt(paidAt);
        return entity;
    }

    private void givenRows(StaffEntity staff, PayrollEntity saved) {
        when(attendanceService.marksFor(eq(CLIENT_ID), any())).thenReturn(Map.of());
        when(attendanceService.employed(eq(CLIENT_ID), any())).thenReturn(List.of(staff));
        when(payrollRepository.findByClientEntity_ClientIdAndPayrollYearAndPayrollMonthAndDeletedAtIsNull(
                eq(CLIENT_ID), anyInt(), anyInt()
        )).thenReturn(saved == null ? List.of() : List.of(saved));
    }

    @Test
    void paidMonthKeepsSavedAmountsAfterTheStaffInsentifChanges() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        StaffEntity staff = staff(700_000);
        givenRows(staff, savedMonth(staff, paidAt));

        PayrollRowDTO row = service.rows(CLIENT_ID, MONTH, TODAY).get(0);

        assertEquals(rp(500_000), row.getInsentif());
        assertEquals(rp(1_740_000), row.getTotal());
        assertEquals(rp(60_000), row.getPotongan());
        assertEquals(1, row.getAbsenceCount());
        assertEquals(paidAt, row.getPaidAt());
    }

    @Test
    void unpaidMonthIsStillCalculatedLive() {
        StaffEntity staff = staff(700_000);
        givenRows(staff, null);

        PayrollRowDTO row = service.rows(CLIENT_ID, MONTH, TODAY).get(0);

        assertEquals(rp(700_000), row.getInsentif());
        assertNull(row.getPaidAt());
    }

    @Test
    void legacyPaidRowWithoutSavedTotalStaysLive() {
        StaffEntity staff = staff(700_000);
        PayrollEntity legacy = savedMonth(staff, LocalDateTime.of(2026, 10, 1, 9, 0));
        legacy.setTotalPayroll(null);
        givenRows(staff, legacy);

        PayrollRowDTO row = service.rows(CLIENT_ID, MONTH, TODAY).get(0);

        assertEquals(rp(700_000), row.getInsentif());
    }
}
