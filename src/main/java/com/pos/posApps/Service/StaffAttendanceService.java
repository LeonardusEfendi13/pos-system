package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.AttendanceItemRequest;
import com.pos.posApps.DTO.Dtos.AttendanceRowDTO;
import com.pos.posApps.DTO.Enum.AttendanceStatus;
import com.pos.posApps.Entity.StaffAttendanceEntity;
import com.pos.posApps.Entity.StaffEntity;
import com.pos.posApps.Repository.StaffAttendanceRepository;
import com.pos.posApps.Repository.StaffRepository;
import com.pos.posApps.Util.PayrollCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StaffAttendanceService {
    private final StaffRepository staffRepository;
    private final StaffAttendanceRepository attendanceRepository;

    public StaffAttendanceService(StaffRepository staffRepository, StaffAttendanceRepository attendanceRepository) {
        this.staffRepository = staffRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public List<AttendanceRowDTO> matrix(Long clientId, YearMonth month) {
        Map<Long, Map<LocalDate, AttendanceStatus>> marks = marksFor(clientId, month);
        List<AttendanceRowDTO> rows = new ArrayList<>();
        for (StaffEntity staff : employed(clientId, month)) {
            Map<LocalDate, AttendanceStatus> staffMarks = marks.getOrDefault(staff.getStaffId(), Map.of());
            int absences = PayrollCalculator.countAbsences(
                    month,
                    staff.getTanggalJoin(),
                    staff.getTanggalResign(),
                    LocalDate.now(),
                    staffMarks
            );
            Map<Integer, String> byDay = new HashMap<>();
            for (Map.Entry<LocalDate, AttendanceStatus> entry : staffMarks.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && YearMonth.from(entry.getKey()).equals(month)) {
                    byDay.put(entry.getKey().getDayOfMonth(), entry.getValue().name());
                }
            }
            rows.add(new AttendanceRowDTO(
                    staff.getStaffId(),
                    staff.getNama(),
                    staff.getJabatan() == null ? "" : staff.getJabatan().name(),
                    staff.getTanggalJoin(),
                    staff.getTanggalResign(),
                    absences,
                    byDay
            ));
        }
        return rows;
    }

    @Transactional
    public void save(Long clientId, List<AttendanceItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new StaffRuleException("Tidak ada perubahan absensi");
        }

        LocalDate today = LocalDate.now();
        for (AttendanceItemRequest item : items) {
            if (item == null || item.getStaffId() == null || item.getDate() == null) {
                throw new StaffRuleException("Data absensi tidak lengkap");
            }
            if (item.getDate().isAfter(today)) {
                throw new StaffRuleException("Absensi tidak bisa diisi untuk tanggal yang akan datang");
            }
            StaffEntity staff = staffRepository
                    .findByStaffIdAndDeletedAtIsNullAndClientEntity_ClientId(item.getStaffId(), clientId)
                    .orElseThrow(() -> new StaffRuleException("Karyawan tidak ditemukan", true));
            if (staff.getTanggalJoin() != null && item.getDate().isBefore(staff.getTanggalJoin())) {
                throw new StaffRuleException("Tanggal absensi sebelum karyawan join");
            }
            if (staff.getTanggalResign() != null && item.getDate().isAfter(staff.getTanggalResign())) {
                throw new StaffRuleException("Tanggal absensi setelah karyawan resign");
            }

            StaffAttendanceEntity existing = attendanceRepository
                    .findByStaffEntity_StaffIdAndAttendanceDate(staff.getStaffId(), item.getDate())
                    .orElse(null);
            if (item.getStatus() == null || item.getStatus().isBlank()) {
                if (existing != null) {
                    attendanceRepository.delete(existing);
                }
                continue;
            }

            AttendanceStatus status;
            try {
                status = AttendanceStatus.valueOf(item.getStatus().trim());
            } catch (IllegalArgumentException exception) {
                throw new StaffRuleException("Status absensi tidak valid");
            }

            StaffAttendanceEntity row = existing == null ? new StaffAttendanceEntity() : existing;
            row.setStaffEntity(staff);
            row.setAttendanceDate(item.getDate());
            row.setStatus(status);
            attendanceRepository.save(row);
        }
    }

    Map<Long, Map<LocalDate, AttendanceStatus>> marksFor(Long clientId, YearMonth month) {
        List<StaffAttendanceEntity> records = attendanceRepository
                .findByStaffEntity_ClientEntity_ClientIdAndAttendanceDateBetween(
                        clientId,
                        month.atDay(1),
                        month.atEndOfMonth()
                );
        Map<Long, Map<LocalDate, AttendanceStatus>> marks = new HashMap<>();
        for (StaffAttendanceEntity record : records) {
            if (record.getStaffEntity() == null || record.getAttendanceDate() == null) {
                continue;
            }
            marks.computeIfAbsent(record.getStaffEntity().getStaffId(), key -> new HashMap<>())
                    .putIfAbsent(record.getAttendanceDate(), record.getStatus());
        }
        return marks;
    }

    List<StaffEntity> employed(Long clientId, YearMonth month) {
        return staffRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNull(clientId).stream()
                .filter(staff -> PayrollCalculator.employedDuring(staff.getTanggalJoin(), staff.getTanggalResign(), month))
                .sorted((left, right) -> {
                    String leftName = left.getNama() == null ? "" : left.getNama();
                    String rightName = right.getNama() == null ? "" : right.getNama();
                    return leftName.compareToIgnoreCase(rightName);
                })
                .toList();
    }
}
