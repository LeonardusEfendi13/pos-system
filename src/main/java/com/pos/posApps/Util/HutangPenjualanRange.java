package com.pos.posApps.Util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class HutangPenjualanRange {
    private static final LocalDateTime UNUSED = LocalDate.of(1970, 1, 1).atStartOfDay();

    public record Bounds(
            LocalDateTime start,
            LocalDateTime end,
            int applyStart,
            int applyEnd,
            String startIso,
            String endIso
    ) {
        public boolean unbounded() {
            return applyStart == 0 && applyEnd == 0;
        }
    }

    private HutangPenjualanRange() {
    }

    public static Bounds resolve(String startDate, String endDate) {
        String start = startDate == null ? "" : startDate.trim();
        String end = endDate == null ? "" : endDate.trim();

        if (start.isEmpty() && end.isEmpty()) {
            return new Bounds(UNUSED, UNUSED, 0, 0, "", "");
        }

        if (start.isEmpty() || end.isEmpty()) {
            throw new IllegalArgumentException(
                    "Tanggal mulai dan tanggal akhir harus diisi bersamaan"
            );
        }

        LocalDate startDay;
        LocalDate endDay;
        try {
            startDay = LocalDate.parse(start);
            endDay = LocalDate.parse(end);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Format tanggal tidak valid");
        }

        if (endDay.isBefore(startDay)) {
            throw new IllegalArgumentException(
                    "Tanggal akhir harus sama atau setelah tanggal mulai"
            );
        }

        return new Bounds(
                startDay.atStartOfDay(),
                endDay.atTime(23, 59, 59),
                1,
                1,
                startDay.toString(),
                endDay.toString()
        );
    }
}
