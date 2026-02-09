package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import java.util.List;
import java.util.Optional;
import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class BranchService {
    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Transactional
    public ResponseInBoolean doCreateBranch(Long customerId, ClientEntity clientData){
        try{
            Optional<CustomerEntity> customerEntity = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(customerId, clientData.getClientId());
            if(customerEntity.isEmpty()){
                return new ResponseInBoolean(false, "Data Customer tidak ditemukan");
            }
            CustomerEntity customerData = customerEntity.get();

            BranchEntity newBranchEntity = new BranchEntity();
            newBranchEntity.setCustomerEntity(customerData);
            branchRepository.save(newBranchEntity);
            return new ResponseInBoolean(true, "Data berhasil disimpan");
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, e.getMessage());
        }

    }
    private BranchDTO convertToDTO(BranchEntity branchs) {
        CustomerEntity fixedCustomerEntity = new CustomerEntity();
        if(branchs.getCustomerEntity() != null){
            fixedCustomerEntity = branchs.getCustomerEntity();
        }
        return new BranchDTO(
                branchs.getBranchId(),
                fixedCustomerEntity.getCustomerId(),
                fixedCustomerEntity.getName(),
                fixedCustomerEntity.getAlamat()
        );
    }

    public List<BranchDTO> getBranchList(){
        List<BranchEntity> branchEntities = branchRepository.findAllByDeletedAtIsNullOrderByBranchIdDesc();
        return branchEntities.stream()
                .map(this::convertToDTO)
                .toList();
    }
    public ResponseInBoolean doDisableBranch(Long branchId){
        Optional<BranchEntity> branchEntityOpt = branchRepository.findAllByBranchIdAndDeletedAtIsNull(branchId);
        if(branchEntityOpt.isEmpty()){
            return new ResponseInBoolean(false, "Data tidak ditemukan");
        }

        BranchEntity branchEntity = branchEntityOpt.get();

        branchEntity.setDeletedAt(getCurrentTimestamp());
        branchRepository.save(branchEntity);
        return new ResponseInBoolean(true, "Data berhasil dihapus");
    }

    public List<CustomerEntity> getAllCabangToko(){
        List<BranchEntity> branchEntityOpt = branchRepository.findAllByDeletedAtIsNullOrderByBranchIdDesc();
        return branchEntityOpt.stream().map(BranchEntity::getCustomerEntity).toList();
    }
}
