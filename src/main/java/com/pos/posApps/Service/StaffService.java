package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.CreateStaffRequest;
import com.pos.posApps.DTO.Dtos.DashboardKaryawanDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.StaffDTO;
import com.pos.posApps.DTO.Enum.Jabatan;
import com.pos.posApps.DTO.Enum.JenisKelamin;
import com.pos.posApps.DTO.Enum.Pendidikan;
import com.pos.posApps.Entity.StaffEntity;
import com.pos.posApps.Repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class StaffService {
    @Autowired
    StaffRepository staffRepository;
    public DashboardKaryawanDTO getDashboardData(){
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
        Long karyawanAktif = staffData.stream().filter(staff-> staff.getTanggalResign() == null).count();
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
    public ResponseInBoolean addStaffData(CreateStaffRequest req){
        System.out.println("req : "+ req);
        try{
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
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            System.out.println("Error di addStatffData : " + e.getMessage());
            return new ResponseInBoolean(false, "Error : "+ e.getMessage());
        }

    }

}
