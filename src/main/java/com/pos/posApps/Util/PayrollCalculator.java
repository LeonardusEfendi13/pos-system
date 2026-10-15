package com.pos.posApps.Util;

import com.pos.posApps.DTO.Enum.AttendanceStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

public final class PayrollCalculator {
    public static final int FREE_ABSENCE_DAYS = 4;

    public record Input(
            BigDecimal gajiPokok,
            BigDecimal uangMakan,
            BigDecimal insentif,
            int daysInMonth,
            int absenceCount
    ) {
    }

    public record Result(
            int daysInMonth,
            int absenceCount,
            int deductedDays,
            BigDecimal dailyRate,
            BigDecimal potongan,
            BigDecimal gajiPokok,
            BigDecimal gajiPokokDibayar,
            BigDecimal uangMakan,
            BigDecimal insentif,
            BigDecimal total
    ) {
    }

    private PayrollCalculator() {
    }

    public static boolean employedDuring(LocalDate join, LocalDate resign, YearMonth month) {
        if (join == null || month == null || join.isAfter(month.atEndOfMonth())) {
            return false;
        }

        return resign == null || !resign.isBefore(month.atDay(1));
    }

    public static boolean countsAsAbsence(AttendanceStatus status) {
        return status == AttendanceStatus.IZIN
                || status == AttendanceStatus.SAKIT
                || status == AttendanceStatus.ALFA;
    }

    public static int countAbsences(
            YearMonth month,
            LocalDate join,
            LocalDate resign,
            Map<LocalDate, AttendanceStatus> marks
    ) {
        if (month == null || marks == null || marks.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Map.Entry<LocalDate, AttendanceStatus> entry : marks.entrySet()) {
            if (isChargeableAbsence(entry.getKey(), entry.getValue(), join, resign, month)) {
                count++;
            }
        }
        return count;
    }

    public static Result calculate(Input input) {
        int days = input == null || input.daysInMonth() < 1 ? 1 : input.daysInMonth();
        BigDecimal pokok = money(input == null ? null : input.gajiPokok());
        BigDecimal makan = money(input == null ? null : input.uangMakan());
        BigDecimal insentif = money(input == null ? null : input.insentif());
        int absences = input == null ? 0 : Math.max(input.absenceCount(), 0);
        int deducted = Math.max(0, absences - FREE_ABSENCE_DAYS);
        BigDecimal daily = pokok.divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
        BigDecimal potongan = daily.multiply(BigDecimal.valueOf(deducted));
        if (potongan.compareTo(pokok) > 0) {
            potongan = pokok;
        }
        BigDecimal dibayar = pokok.subtract(potongan);
        BigDecimal total = dibayar.add(makan).add(insentif);
        return new Result(days, absences, deducted, daily, potongan, pokok, dibayar, makan, insentif, total);
    }

    private static boolean isChargeableAbsence(
            LocalDate date,
            AttendanceStatus status,
            LocalDate join,
            LocalDate resign,
            YearMonth month
    ) {
        if (date == null || !countsAsAbsence(status) || !YearMonth.from(date).equals(month)) {
            return false;
        }
        if (join != null && date.isBefore(join)) {
            return false;
        }
        return resign == null || !date.isAfter(resign);
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }
}
