package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.AttendanceStatus;
import com.pos.posApps.Entity.StaffAttendanceEntity;
import com.pos.posApps.Entity.StaffEntity;
import com.pos.posApps.Repository.StaffAttendanceRepository;
import com.pos.posApps.Repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class AttendanceService {
    @Autowired
    private StaffAttendanceRepository attendanceRepository;

    @Autowired
    private StaffRepository staffRepository;

    public List<AttendanceHistoryDTO> getMonthlyAttendanceMatrix(LocalDateTime startDate, LocalDateTime endDate) {
        // 1. Ambil semua karyawan yang aktif (belum resign)
        List<StaffEntity> activeStaffList = staffRepository.findByTanggalResignIsNull();
        List<AttendanceHistoryDTO> resultMatrix = new ArrayList<>();

        // 2. Ambil seluruh data absensi di bulan ini secara massal (1x Query ke DB)
        List<StaffAttendanceEntity> attendanceList = attendanceRepository.findByAttendanceDateBetween(startDate, endDate);

        // Grouping data absensi berdasarkan staffId agar mudah dicari di memori
        Map<Long, List<StaffAttendanceEntity>> attendanceMapByStaff = attendanceList.stream()
                .collect(Collectors.groupingBy(a -> a.getStaffEntity().getStaffId()));

        // Dapatkan jumlah hari maksimum dalam bulan tersebut secara dinamis berdasarkan startDate
        int maxDaysInMonth = startDate.toLocalDate().lengthOfMonth();

        // 3. Looping per karyawan untuk memetakan tanggal 1 s/d maxDaysInMonth
        for (StaffEntity staff : activeStaffList) {
            AttendanceHistoryDTO row = new AttendanceHistoryDTO();
            row.setStaffId(staff.getStaffId());
            row.setNamaStaff(staff.getNama());

            Map<Integer, String> statusMap = new HashMap<>();
            long hadir = 0, sakit = 0, izin = 0, alfa = 0;

            // Ambil histori absen khusus untuk staff ini
            List<StaffAttendanceEntity> staffRecords = attendanceMapByStaff.getOrDefault(staff.getStaffId(), new ArrayList<>());

            // PERBAIKAN: Gunakan toMap dengan merge function (jika double, ambil record pertama)
            Map<Integer, String> dayToStatusMap = staffRecords.stream()
                    .collect(Collectors.toMap(
                            att -> att.getAttendanceDate().getDayOfMonth(), // Key: Tanggal (1-31)
                            att -> att.getStatus() != null ? att.getStatus().name().toUpperCase() : "-", // Value: Nama Enum (pasti UPPERCASE)
                            (existing, replacement) -> existing // Merge function: jika tanggal sama, pertahankan yang lama
                    ));

            // Isi data dari tanggal 1 sampai 31 (atau 28, 29, 30 sesuai bulan terkait)
            for (int day = 1; day <= 31; day++) {
                // Kasus jika bulan terkait tidak sampai tanggal 31 (misal: Februari atau Juni)
                if (day > maxDaysInMonth) {
                    statusMap.put(day, "-");
                    continue;
                }

                // Cek status dari database, jika tidak ada pasang "-"
                String dbStatus = dayToStatusMap.getOrDefault(day, "-");

                switch (dbStatus) {
                    case "HADIR" -> { statusMap.put(day, "H"); hadir++; }
                    case "SAKIT" -> { statusMap.put(day, "S"); sakit++; }
                    case "IZIN"  -> { statusMap.put(day, "I"); izin++; }
                    case "ALFA"  -> { statusMap.put(day, "A"); alfa++; }
                    default      -> statusMap.put(day, "-");
                }
            }

            row.setStatusPerTanggal(statusMap);
            row.setTotalHadir(hadir);
            row.setTotalSakit(sakit);
            row.setTotalIzin(izin);
            row.setTotalAlfa(alfa);

            resultMatrix.add(row);
        }

        return resultMatrix;
    }

    @Transactional
    public ResponseInBoolean saveBatchAttendance(AttendanceBatchForm form) {
        try{
            if (form.getAttendances() == null || form.getAttendances().isEmpty()) {
                throw new IllegalArgumentException("Data absensi kosong.");
            }

            // Dapatkan rentang waktu hari ini (00:00:00 sampai 23:59:59)
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);

            for (AttendanceBatchForm.AttendanceItem item : form.getAttendances()) {
                Optional<StaffEntity> staffOpt = staffRepository.findFirstByStaffIdAndDeletedAtIsNull(item.getStaffId());
                if(staffOpt.isEmpty()){
                    return new ResponseInBoolean(false, "Data karyawan tidak ditemukan");
                }
                StaffEntity staff = staffOpt.get();

                // Cek apakah karyawan ini sudah punya catatan absen hari ini
                Optional<StaffAttendanceEntity> existingAttendance = attendanceRepository.findByStaffEntityAndAttendanceDateBetween(staff, startOfDay, endOfDay);

                StaffAttendanceEntity attendance;
                if (existingAttendance.isPresent()) {
                    // Jika sudah ada, kita update statusnya (mencegah duplikasi data baru)
                    attendance = existingAttendance.get();
                } else {
                    // Jika belum ada, buat record baru
                    attendance = new StaffAttendanceEntity();
                    attendance.setStaffEntity(staff);
                    attendance.setAttendanceDate(getCurrentTimestamp());
                    // Anda bisa set data clientEntity/perusahaan jika diperlukan di sini
                }

                // Set status berdasarkan input radio button (Mengonversi String ke Enum)
                attendance.setStatus(AttendanceStatus.valueOf(item.getStatus()));

                // Simpan ke Database
                attendanceRepository.save(attendance);
            }
            return new ResponseInBoolean(true, "Berhasil menyimpan data absensi karyawan");
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            System.out.println("Error when trying to save batch attendance : " + e.getMessage() );
            return new ResponseInBoolean(false, "Error : " + e.getMessage());
        }
    }

    @Transactional
    public ResponseInBoolean updateSingleStaff(Long staffId, LocalDate tanggal, String status) {
        try {
            Optional<StaffEntity> staffEntityOpt = staffRepository.findFirstByStaffIdAndDeletedAtIsNull(staffId);

            if (staffEntityOpt.isEmpty()) {
                return new ResponseInBoolean(false, "Data karyawan tidak ditemukan");
            }

            StaffEntity staff = staffEntityOpt.get();

            LocalDateTime startDate = tanggal.atStartOfDay();
            LocalDateTime endDate = tanggal.atTime(23, 59, 59);

            Optional<StaffAttendanceEntity> attendanceOpt =
                    attendanceRepository.findByStaffEntityAndAttendanceDateBetween(
                            staff, startDate, endDate);

            StaffAttendanceEntity attendance;

            if (attendanceOpt.isPresent()) {
                // Update existing attendance
                attendance = attendanceOpt.get();
            } else {
                System.out.println("Otw create data");
                // Create new attendance
                attendance = new StaffAttendanceEntity();
                attendance.setStaffEntity(staff);
                attendance.setAttendanceDate(startDate); // or LocalDateTime.now() if preferred
            }

            attendance.setStatus(AttendanceStatus.valueOf(status));

            attendanceRepository.save(attendance);

            return new ResponseInBoolean(
                    true,
                    attendanceOpt.isPresent()
                            ? "Berhasil memperbarui status absensi karyawan : " + staff.getNama()
                            : "Berhasil membuat absensi karyawan : " + staff.getNama()
            );

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Error : " + e.getMessage());
        }
    }
}