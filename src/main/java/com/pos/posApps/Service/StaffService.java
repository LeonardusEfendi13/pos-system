package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.DashboardKaryawanDTO;
import com.pos.posApps.DTO.Dtos.StaffDTO;
import com.pos.posApps.Entity.StaffEntity;
import com.pos.posApps.Repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class StaffService {
    @Autowired
    StaffRepository staffRepository;
    public DashboardKaryawanDTO getDashboardData(){
        List<StaffEntity> staffEntityList = staffRepository.findAllByDeletedAtIsNull();
        List<StaffDTO> staffData = staffEntityList.stream().sorted(Comparator.comparing(StaffEntity::getNama))
                .map(staff-> new StaffDTO(
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

//    public List<VehicleEntity> getVehicleList(String brand){
//        if(brand == null || brand.isBlank()){
//            return vehicleRepository.findAllOrderByModelAsc();
//        }
//        return vehicleRepository.findAllByBrandOrderByModelAsc(brand);
//    }
//
//    @Transactional
//    public ResponseInBoolean insertVehicle(String name, String brand, String partNumber){
//        try{
//            Optional<VehicleEntity> vehicleEntityOpt = vehicleRepository.findFirstByModelIgnoreCaseAndBrandIgnoreCase(name, brand);
//            if(vehicleEntityOpt.isPresent()){
//                //Berarti data udah ada
//                return new ResponseInBoolean(true, "Data Sudah ada");
//            }
//
//            VehicleEntity vehicleEntity = new VehicleEntity();
//            vehicleEntity.setBrand(brand);
//            vehicleEntity.setModel(name);
//            vehicleEntity.setKnownPartNumber(partNumber);
//            vehicleRepository.save(vehicleEntity);
//            return new ResponseInBoolean(true, "Data Berhasil dibuat");
//        }catch (Exception e){
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            return new ResponseInBoolean(false, "Gagal insert data, hubungi admin : " + e.getMessage());
//        }
//    }
//
//    @Transactional
//    public ResponseInBoolean editVehicle(Long vehicleId, String name, String brand, String partNumber){
//        try{
//            //Will throw exception when data is null
//            VehicleEntity vehicleEntity = vehicleRepository.findFirstById(vehicleId);
//
//            // Check if another vehicle with the same name & brand exists (excluding the current one)
//            boolean vehicleExists = vehicleRepository.existsByModelIgnoreCaseAndBrandIgnoreCaseAndIdNot(name, brand, vehicleId);
//
//            if (vehicleExists) {
//                return new ResponseInBoolean(true, "Data sudah ada"); // Duplicate data exists
//            }
//
//            vehicleEntity.setModel(name);
//            vehicleEntity.setBrand(brand);
//            vehicleEntity.setKnownPartNumber(partNumber);
//            vehicleRepository.save(vehicleEntity);
//            return new ResponseInBoolean(true, "Berhasil update data");
//        }catch (Exception e){
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            return new ResponseInBoolean(false, "Gagal update data");
//        }
//    }
//
//    @Transactional
//    public Boolean deleteVehicle(Long vehicleId){
//        try{
//            //Will throw exception when data is null
//            vehicleRepository.deleteById(vehicleId);
//            return true;
//        }catch (Exception e){
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            return false;
//        }
//    }
}
