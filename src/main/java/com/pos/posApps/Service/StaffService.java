package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.Jabatan;
import com.pos.posApps.DTO.Enum.JenisKelamin;
import com.pos.posApps.DTO.Enum.JenisTransaksiInsentive;
import com.pos.posApps.DTO.Enum.Pendidikan;
import com.pos.posApps.Entity.InsentiveLogEntity;
import com.pos.posApps.Entity.KomponenGajiEntity;
import com.pos.posApps.Entity.PayrollEntity;
import com.pos.posApps.Entity.StaffEntity;
import com.pos.posApps.Repository.InsentiveLogRepository;
import com.pos.posApps.Repository.KomponenGajiRepository;
import com.pos.posApps.Repository.PayrollRepository;
import com.pos.posApps.Repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StaffService {
    @Autowired
    StaffRepository staffRepository;

    @Autowired
    KomponenGajiRepository komponenGajiRepository;

    @Autowired
    InsentiveLogRepository insentiveLogRepository;

    @Autowired
    PayrollRepository payrollRepository;

    public DashboardKaryawanDTO getDashboardData() {
        List<StaffEntity> staffEntityList = staffRepository.findAllByDeletedAtIsNull();
        List<StaffDTO> staffData = staffEntityList.stream()
                .sorted(
                        // 1. Urutkan berdasarkan status resign (null diposisikan paling awal/atas)
                        Comparator.comparing(StaffEntity::getTanggalResign, Comparator.nullsFirst(Comparator.naturalOrder()))
                                // 2. Jika sama-sama aktif atau sama-sama resign, urutkan berdasarkan nama alfabetis
                                .thenComparing(StaffEntity::getNama)
                )
                .map(staff -> new StaffDTO(
                        staff.getStaffId(),
                        staff.getNama(),
                        staff.getNik(),
                        staff.getTempatLahir(),
                        staff.getTanggalLahir(),
                        staff.getTanggalJoin(),
                        staff.getTanggalResign(),
                        staff.getJabatan().toString(),
                        staff.getGaji(),
                        staff.getNoHp(),
                        staff.getNoHpDarurat(),
                        staff.getJenisKelamin().toString(),
                        staff.getPendidikanTerakhir().toString()
                )).toList();
        Long totalKaryawan = (long) staffData.size();
        Long karyawanAktif = staffData.stream().filter(staff -> staff.getTanggalResign() == null).count();
        Long karyawanResign = staffData.stream().filter(staff -> staff.getTanggalResign() != null).count();
        BigDecimal payroll = staffData.stream().filter(staff -> staff.getTanggalResign() == null).map(StaffDTO::getGaji).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardKaryawanDTO(
                totalKaryawan,
                karyawanAktif,
                karyawanResign,
                payroll,
                staffData
        );
    }

    @Transactional
    public ResponseInBoolean addStaffData(CreateStaffRequest req) {
        System.out.println("req : " + req);
        try {
            LocalDateTime tglLahir = LocalDate.parse(req.getTanggalLahir()).atStartOfDay();
            LocalDateTime tglJoin = LocalDate.parse(req.getTanggalJoin()).atStartOfDay();
            StaffEntity staffEntity = new StaffEntity();
            staffEntity.setNik(req.getNik());
            staffEntity.setNama(req.getNama());
            staffEntity.setTempatLahir(req.getTempatLahir());
            staffEntity.setTanggalLahir(tglLahir);
            staffEntity.setNoHp(req.getNoHp());
            staffEntity.setNoHpDarurat(req.getNoHpDarurat());
            staffEntity.setJabatan(Jabatan.valueOf(req.getJabatan()));
            staffEntity.setJenisKelamin(JenisKelamin.valueOf(req.getJenisKelamin()));
            staffEntity.setTanggalJoin(tglJoin);
            staffEntity.setGaji(BigDecimal.valueOf(Long.parseLong(req.getGaji())));
            staffEntity.setPendidikanTerakhir(Pendidikan.valueOf(req.getPendidikanTerakhir()));
            staffRepository.save(staffEntity);
            System.out.println("All ok");
            return new ResponseInBoolean(true, "Berhasil menambahkan karyawan");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            System.out.println("Error di addStatffData : " + e.getMessage());
            return new ResponseInBoolean(false, "Error : " + e.getMessage());
        }
    }

    public StaffDTO getStaffDetail(Long staffId) {
        try {
            Optional<StaffEntity> staffDataopt = staffRepository.findFirstByStaffIdAndDeletedAtIsNull(staffId);
            if(staffDataopt.isEmpty()){
                return new StaffDTO();
            }else {
                StaffEntity staffData = staffDataopt.get();
                return new StaffDTO(
                        staffData.getStaffId(),
                        staffData.getNama(),
                        staffData.getNik(),
                        staffData.getTempatLahir(),
                        staffData.getTanggalLahir(),
                        staffData.getTanggalJoin(),
                        staffData.getTanggalResign(),
                        staffData.getJabatan().toString(),
                        staffData.getGaji(),
                        staffData.getNoHp(),
                        staffData.getNoHpDarurat(),
                        staffData.getJenisKelamin().toString(),
                        staffData.getPendidikanTerakhir().toString()
                );
            }
        } catch (Exception e) {
            System.out.println("Error getStaff Detail : " + e.getMessage());
            return new StaffDTO();
        }
    }

    @Transactional
    public ResponseInBoolean editStaffData(Long staffId, EditStaffRequest req) {
        System.out.println("req : " + req);
        try {
            Optional<StaffEntity> staffEntityOpt = staffRepository.findFirstByStaffIdAndDeletedAtIsNull(staffId);
            if(staffEntityOpt.isEmpty()){
                return new ResponseInBoolean(false, "Data karyawan tidak ditemukan");
            }
            StaffEntity staffEntity = staffEntityOpt.get();
            LocalDateTime tglLahir = LocalDate.parse(req.getTanggalLahir()).atStartOfDay();
            LocalDateTime tglJoin = LocalDate.parse(req.getTanggalJoin()).atStartOfDay();
            LocalDateTime tglResign;
            if(req.getTanggalResign() == null || req.getTanggalResign().isEmpty()){
                tglResign = null;
            }else{
                tglResign = LocalDate.parse(req.getTanggalResign()).atStartOfDay();
            }
            staffEntity.setNik(req.getNik());
            staffEntity.setNama(req.getNama());
            staffEntity.setTempatLahir(req.getTempatLahir());
            staffEntity.setTanggalLahir(tglLahir);
            staffEntity.setNoHp(req.getNoHp());
            staffEntity.setNoHpDarurat(req.getNoHpDarurat());
            staffEntity.setJabatan(Jabatan.valueOf(req.getJabatan()));
            staffEntity.setJenisKelamin(JenisKelamin.valueOf(req.getJenisKelamin()));
            staffEntity.setTanggalJoin(tglJoin);
            staffEntity.setTanggalResign(tglResign);
            staffEntity.setGaji(BigDecimal.valueOf(Long.parseLong(req.getGaji())));
            staffEntity.setPendidikanTerakhir(Pendidikan.valueOf(req.getPendidikanTerakhir()));
            staffRepository.save(staffEntity);
            System.out.println("All ok");
            return new ResponseInBoolean(true, "Berhasil edit karyawan : " + req.getNama());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            System.out.println("Error di addStatffData : " + e.getMessage());
            return new ResponseInBoolean(false, "Error : " + e.getMessage());
        }
    }

    public List<StaffDTO> getStaffData(){
        List<StaffEntity> staffEntityList = staffRepository.findAllByDeletedAtIsNull();
        List<StaffDTO> staffData = staffEntityList.stream()
                .sorted(
                        // 1. Urutkan berdasarkan status resign (null diposisikan paling awal/atas)
                        Comparator.comparing(StaffEntity::getTanggalResign, Comparator.nullsFirst(Comparator.naturalOrder()))
                                // 2. Jika sama-sama aktif atau sama-sama resign, urutkan berdasarkan nama alfabetis
                                .thenComparing(StaffEntity::getNama)
                )
                .map(staff -> new StaffDTO(
                        staff.getStaffId(),
                        staff.getNama(),
                        staff.getNik(),
                        staff.getTempatLahir(),
                        staff.getTanggalLahir(),
                        staff.getTanggalJoin(),
                        staff.getTanggalResign(),
                        staff.getJabatan().toString(),
                        staff.getGaji(),
                        staff.getNoHp(),
                        staff.getNoHpDarurat(),
                        staff.getJenisKelamin().toString(),
                        staff.getPendidikanTerakhir().toString()
                )).toList();

        return staffData;
    }

    public List<KomponenGajiEntity> getAllActiveKomponenGaji(){
        return komponenGajiRepository.findByIsActiveAndDeletedAtIsNull(true);
    }

    public List<InsentiveLogDTO> getAllInsentiveBalance(){
        List<StaffEntity> allStaff = staffRepository.findAllByDeletedAtIsNull();
        return allStaff.stream().map(staff->{
            BigDecimal totalPencairan = insentiveLogRepository.sumNominalByStaffIdAndJenis(staff.getStaffId(), JenisTransaksiInsentive.PENCAIRAN);
            BigDecimal totalSetoran = insentiveLogRepository.sumNominalByStaffIdAndJenis(staff.getStaffId(), JenisTransaksiInsentive.SETORAN);
            BigDecimal currentBalance = totalSetoran.subtract(totalPencairan);

            return new InsentiveLogDTO(
                    staff.getStaffId(),
                    staff.getNama(),
                    staff.getJabatan(),
                    currentBalance
            );
        }).collect(Collectors.toList());
    }

    public List<PayrollHistoryDTO> getPayrollHistory(){
        Pageable pageable = PageRequest.of(0, 10);
        List<PayrollEntity> payrollEntities = payrollRepository.findByDeletedAtIsNullOrderByCreatedAtDesc(pageable);
        return payrollEntities.stream().map(p -> new PayrollHistoryDTO(
                p.getPayrollId(),
                p.getPayrollMonth(),
                p.getPayrollYear(),
                p.getStaffEntity() != null ? p.getStaffEntity().getNama() : "-",
                p.getTotalPayroll()
        )).collect(Collectors.toList());
    }

}
