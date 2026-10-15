package com.pos.posApps.Util;

import com.pos.posApps.DTO.Enum.AttendanceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollCalculatorTest {
    @Test
    void zeroAbsencesPaysEveryComponent() {
        PayrollCalculator.Result result = PayrollCalculator.calculate(new PayrollCalculator.Input(
                new BigDecimal("3100000"),
                new BigDecimal("500000"),
                new BigDecimal("200000"),
                31,
                0
        ));

        assertEquals(0, result.deductedDays());
        assertEquals(new BigDecimal("100000"), result.dailyRate());
        assertEquals(new BigDecimal("0"), result.potongan());
        assertEquals(new BigDecimal("3800000"), result.total());
    }

    @Test
    void fourAbsencesDoNotDeduct() {
        PayrollCalculator.Result result = PayrollCalculator.calculate(new PayrollCalculator.Input(
                new BigDecimal("3100000"),
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                31,
                4
        ));

        assertEquals(0, result.deductedDays());
        assertEquals(new BigDecimal("0"), result.potongan());
        assertEquals(new BigDecimal("3200000"), result.total());
    }

    @Test
    void fifthAbsenceDeductsOneDailyRateFromBaseSalaryOnly() {
        PayrollCalculator.Result result = PayrollCalculator.calculate(new PayrollCalculator.Input(
                new BigDecimal("3100000"),
                new BigDecimal("500000"),
                new BigDecimal("200000"),
                31,
                6
        ));

        assertEquals(2, result.deductedDays());
        assertEquals(new BigDecimal("100000"), result.dailyRate());
        assertEquals(new BigDecimal("200000"), result.potongan());
        assertEquals(new BigDecimal("2900000"), result.gajiPokokDibayar());
        assertEquals(new BigDecimal("500000"), result.uangMakan());
        assertEquals(new BigDecimal("200000"), result.insentif());
        assertEquals(new BigDecimal("3600000"), result.total());
    }

    @Test
    void roundsDailyRateHalfUpToRupiah() {
        PayrollCalculator.Result result = PayrollCalculator.calculate(new PayrollCalculator.Input(
                new BigDecimal("3000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                31,
                5
        ));

        assertEquals(new BigDecimal("96774"), result.dailyRate());
        assertEquals(1, result.deductedDays());
        assertEquals(new BigDecimal("96774"), result.potongan());
        assertEquals(new BigDecimal("2903226"), result.gajiPokokDibayar());
    }

    @Test
    void capsDeductionAtBaseSalary() {
        PayrollCalculator.Result result = PayrollCalculator.calculate(new PayrollCalculator.Input(
                new BigDecimal("2"),
                new BigDecimal("10000"),
                new BigDecimal("5000"),
                3,
                7
        ));

        assertEquals(new BigDecimal("1"), result.dailyRate());
        assertEquals(new BigDecimal("2"), result.potongan());
        assertEquals(new BigDecimal("0"), result.gajiPokokDibayar());
        assertEquals(new BigDecimal("15000"), result.total());
    }

    @Test
    void unmarkedDaysDoNotCountAndSickLeaveDoes() {
        YearMonth october = YearMonth.of(2026, 10);
        Map<LocalDate, AttendanceStatus> marks = new HashMap<>();
        marks.put(LocalDate.of(2026, 10, 1), AttendanceStatus.HADIR);
        marks.put(LocalDate.of(2026, 10, 2), AttendanceStatus.SAKIT);
        marks.put(LocalDate.of(2026, 10, 3), AttendanceStatus.IZIN);
        marks.put(LocalDate.of(2026, 10, 4), AttendanceStatus.ALFA);

        assertEquals(3, PayrollCalculator.countAbsences(
                october,
                LocalDate.of(2026, 10, 1),
                null,
                marks
        ));
    }

    @Test
    void ignoresAbsenceOutsideEmployment() {
        YearMonth october = YearMonth.of(2026, 10);
        Map<LocalDate, AttendanceStatus> marks = new HashMap<>();
        marks.put(LocalDate.of(2026, 10, 1), AttendanceStatus.SAKIT);
        marks.put(LocalDate.of(2026, 10, 20), AttendanceStatus.ALFA);

        assertEquals(0, PayrollCalculator.countAbsences(
                october,
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 15),
                marks
        ));
        assertEquals(true, PayrollCalculator.employedDuring(
                LocalDate.of(2026, 10, 31),
                null,
                october
        ));
        assertEquals(false, PayrollCalculator.employedDuring(
                LocalDate.of(2026, 11, 1),
                null,
                october
        ));
        assertEquals(false, PayrollCalculator.employedDuring(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                october
        ));
    }
}
