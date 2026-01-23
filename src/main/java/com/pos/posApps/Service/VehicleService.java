package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.VehicleEntity;
import com.pos.posApps.Repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {
    @Autowired
    VehicleRepository vehicleRepository;

    public List<VehicleEntity> getVehicleList(String brand){
        if(brand == null || brand.isBlank()){
            System.out.println("Gada brand");
            return vehicleRepository.findAll();
        }
        System.out.println("ada brand");
        return vehicleRepository.findAllByBrand(brand);
    }

    @Transactional
    public ResponseInBoolean insertVehicle(String name, String brand, String partNumber){
        try{
            Optional<VehicleEntity> vehicleEntityOpt = vehicleRepository.findFirstByModelIgnoreCaseAndBrandIgnoreCase(name, brand);
            if(vehicleEntityOpt.isPresent()){
                //Berarti data udah ada
                return new ResponseInBoolean(true, "Data Sudah ada");
            }

            VehicleEntity vehicleEntity = new VehicleEntity();
            vehicleEntity.setBrand(brand);
            vehicleEntity.setModel(name);
            vehicleEntity.setKnownPartNumber(partNumber);
            vehicleRepository.save(vehicleEntity);
            return new ResponseInBoolean(true, "Data Berhasil dibuat");
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Gagal insert data, hubungi admin : " + e.getMessage());
        }
    }

    @Transactional
    public Boolean editVehicle(Long vehicleId, String name, String brand, String partNumber){
        try{
            //Will throw exception when data is null
            VehicleEntity vehicleEntity = vehicleRepository.findFirstById(vehicleId);

            // Check if another vehicle with the same name & brand exists (excluding the current one)
            boolean vehicleExists = vehicleRepository.existsByModelIgnoreCaseAndBrandIgnoreCase(name, brand);

            if (vehicleExists) {
                return false; // Duplicate data exists
            }

            vehicleEntity.setModel(name);
            vehicleEntity.setBrand(brand);
            vehicleEntity.setKnownPartNumber(partNumber);
            vehicleRepository.save(vehicleEntity);
            return true;
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    @Transactional
    public Boolean deleteVehicle(Long vehicleId){
        try{
            //Will throw exception when data is null
            vehicleRepository.deleteById(vehicleId);
            return true;
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }
}
