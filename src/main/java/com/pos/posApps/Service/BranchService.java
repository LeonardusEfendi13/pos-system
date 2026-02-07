package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class BranchService {
    @Autowired
    AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LoginTokenRepository loginTokenRepository;

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

    public boolean doUpdatBranch(EditUserRequest request){
        AccountEntity accountEntity = accountRepository.findByAccountIdAndDeletedAtIsNull(request.getId());
        if(accountEntity != null){
            accountEntity.setName(request.getName());
            accountEntity.setUsername(request.getUsername());
            accountEntity.setUsername(request.getUsername());
            accountEntity.setRole(request.getRole());
            accountRepository.save(accountEntity);
            return true;
        }else{
            return false;
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
//        List<AccountEntity> accountEntities = accountRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByAccountIdDesc(clientId);
        List<BranchEntity> branchEntities = branchRepository.findAllByDeletedAtIsNullOrderByBranchIdDesc();
        return branchEntities.stream()
                .map(this::convertToDTO)
                .toList();
    }
    public UserDTO getCurrentLoggedInUser(String token){
        Optional<LoginTokenEntity> loginTokenEntityOptional = loginTokenRepository.findByTokenAndDeletedAtIsNull(token);
        if(loginTokenEntityOptional.isEmpty()){
            return null;
        }

        LoginTokenEntity loginTokenEntity = loginTokenEntityOptional.get();
        Long userId = loginTokenEntity.getAccountEntity().getAccountId();
        AccountEntity accountEntity = accountRepository.findByAccountIdAndDeletedAtIsNull(userId);
        return new UserDTO(
                accountEntity.getAccountId(),
                accountEntity.getName(),
                accountEntity.getUsername(),
                accountEntity.getRole()
        );
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
}
