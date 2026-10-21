package com.pos.posApps.Util;

import com.pos.posApps.DTO.Enum.AttendanceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollCalculatorTest {

    private static BigDecimal rp(long value) {
        return BigDecimal.valueOf(value);
    }

    @Test
    void deductsEveryAbsenceFromPokokAndMakanOnly() {
        PayrollCalculator.Result result = PayrollCalculator.calculate(new PayrollCalculator.Input(
                rp(1_500_000), rp(300_000), rp(200_000), 31, 31, 29, 2
        ));

        assertEquals(rp(1_800_000), result.totalGaji());
        assertEquals(2, result.deductedDays());
        assertEquals(rp(58_065), result.dailyRate());
        assertEquals(rp(116_130), result.potongan());
        assertEquals(rp(1_683_870), result.total());
        // Insentif is saved in full, untouched by absences
        assertEquals(rp(200_000), result.insentif());
    }

    @Test
    void paysTheFullSalaryWithoutAbsences() {
        PayrollCalculator.Result result = PayrollCalculator.calculate(new PayrollCalculator.Input(
                rp(1_500_000), rp(300_000), rp(200_000), 31, 31, 31, 0
        ));

        assertEquals(rp(0), result.potongan());
        assertEquals(rp(1_800_000), result.total());
    }

    @Test
    void countsPresentAndAbsentDaysUpToToday() {
        YearMonth month = YearMonth.of(2026, 10);
        LocalDate join = LocalDate.of(2026, 9, 1);
        LocalDate today = LocalDate.of(2026, 10, 5);
        Map<LocalDate, AttendanceStatus> marks = Map.of(
                LocalDate.of(2026, 10, 1), AttendanceStatus.HADIR,
                LocalDate.of(2026, 10, 2), AttendanceStatus.HADIR,
                LocalDate.of(2026, 10, 3), AttendanceStatus.IZIN
        );

        // The 4th is unmarked and past (alfa); today is unmarked and not counted yet
        assertEquals(2, PayrollCalculator.countPresent(month, join, null, today, marks));
        assertEquals(2, PayrollCalculator.countAbsences(month, join, null, today, marks));
    }
}
