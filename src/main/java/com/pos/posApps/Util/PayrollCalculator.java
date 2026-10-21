package com.pos.posApps.Util;

import com.pos.posApps.DTO.Enum.AttendanceStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public final class PayrollCalculator {
    public record Input(
            BigDecimal gajiPokok,
            BigDecimal uangMakan,
            BigDecimal insentif,
            int daysInMonth,
            int workedDays,
            int presentCount,
            int absenceCount
    ) {
    }

    public record Result(
            int daysInMonth,
            int workedDays,
            int presentCount,
            int absenceCount,
            int deductedDays,
            BigDecimal gajiPokok,
            BigDecimal uangMakan,
            BigDecimal insentif,
            BigDecimal totalGaji,
            BigDecimal dailyRate,
            BigDecimal gajiTerkumpul,
            BigDecimal potongan,
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

    /**
     * Days of the month the salary has built up for: from the 1st (or join date)
     * through the earliest of month end, resign date, and asOf (payday is the 1st,
     * so a running month only pays up to today).
     */
    public static int workedDays(YearMonth month, LocalDate join, LocalDate resign, LocalDate asOf) {
        LocalDate[] range = workedRange(month, join, resign, asOf);
        if (range == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(range[0], range[1]) + 1;
    }

    /**
     * Izin, sakit, and alfa count, and so does any worked day left unmarked once
     * it has passed (treated as alfa). Today only counts after it is marked.
     */
    public static int countAbsences(
            YearMonth month,
            LocalDate join,
            LocalDate resign,
            LocalDate asOf,
            Map<LocalDate, AttendanceStatus> marks
    ) {
        LocalDate[] range = workedRange(month, join, resign, asOf);
        if (range == null) {
            return 0;
        }

        Map<LocalDate, AttendanceStatus> safeMarks = marks == null ? Map.of() : marks;
        int count = 0;
        for (LocalDate date = range[0]; !date.isAfter(range[1]); date = date.plusDays(1)) {
            AttendanceStatus status = safeMarks.get(date);
            boolean absent = status == null
                    ? asOf == null || date.isBefore(asOf)
                    : countsAsAbsence(status);
            if (absent) {
                count++;
            }
        }
        return count;
    }

    /** Worked days marked hadir; an unmarked today is neither present nor absent yet. */
    public static int countPresent(
            YearMonth month,
            LocalDate join,
            LocalDate resign,
            LocalDate asOf,
            Map<LocalDate, AttendanceStatus> marks
    ) {
        LocalDate[] range = workedRange(month, join, resign, asOf);
        if (range == null || marks == null) {
            return 0;
        }

        int count = 0;
        for (LocalDate date = range[0]; !date.isAfter(range[1]); date = date.plusDays(1)) {
            if (marks.get(date) == AttendanceStatus.HADIR) {
                count++;
            }
        }
        return count;
    }

    // From the 1st (or join) to the earliest of month end, resign, and asOf
    private static LocalDate[] workedRange(YearMonth month, LocalDate join, LocalDate resign, LocalDate asOf) {
        if (month == null) {
            return null;
        }

        LocalDate start = month.atDay(1);
        if (join != null && join.isAfter(start)) {
            start = join;
        }
        LocalDate end = month.atEndOfMonth();
        if (resign != null && resign.isBefore(end)) {
            end = resign;
        }
        if (asOf != null && asOf.isBefore(end)) {
            end = asOf;
        }
        return end.isBefore(start) ? null : new LocalDate[] {start, end};
    }

    /**
     * Total gaji = pokok + makan, earned per day of the month. Every absence costs
     * one daily rate. Insentif is not part of the salary: it is saved in full each
     * paid month and cashed out separately.
     */
    public static Result calculate(Input input) {
        int days = input == null || input.daysInMonth() < 1 ? 1 : input.daysInMonth();
        int worked = input == null ? 0 : Math.min(Math.max(input.workedDays(), 0), days);
        BigDecimal pokok = money(input == null ? null : input.gajiPokok());
        BigDecimal makan = money(input == null ? null : input.uangMakan());
        BigDecimal insentif = money(input == null ? null : input.insentif());
        BigDecimal totalGaji = pokok.add(makan);
        int present = input == null ? 0 : Math.max(input.presentCount(), 0);
        int absences = input == null ? 0 : Math.max(input.absenceCount(), 0);
        int deducted = absences;
        BigDecimal daily = totalGaji.divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
        // A full month pays the exact total, not a rounded daily rate times days
        BigDecimal terkumpul = worked == days
                ? totalGaji
                : daily.multiply(BigDecimal.valueOf(worked));
        BigDecimal potongan = daily.multiply(BigDecimal.valueOf(deducted));
        if (potongan.compareTo(terkumpul) > 0) {
            potongan = terkumpul;
        }
        BigDecimal total = terkumpul.subtract(potongan);
        return new Result(
                days,
                worked,
                present,
                absences,
                deducted,
                pokok,
                makan,
                insentif,
                totalGaji,
                daily,
                terkumpul,
                potongan,
                total
        );
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }
}
