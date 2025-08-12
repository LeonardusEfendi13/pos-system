package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.EditUserRequest;
import com.pos.posApps.DTO.Dtos.RegisterRequest;
import com.pos.posApps.DTO.Dtos.UserDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Repository.AccountRepository;
import com.pos.posApps.Repository.ClientRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class AccountService {
    @Autowired
    AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public boolean doCreateAccount(RegisterRequest request, String clientId){
        try{
            AccountEntity accountEntity = accountRepository.findByUsernameAndDeletedAtIsNull(request.getUsername());
            if(accountEntity != null){
                return false;
            }
            String hashedPassword = passwordEncoder.encode(request.getPassword());
            String lastAccountId = accountRepository.findFirstByOrderByAccountIdDesc().getAccountId();
            String newAccountId = Generator.generateId(lastAccountId == null ? "ACC0" : lastAccountId);

            ClientEntity clientEntity = clientRepository.findByClientIdAndDeletedAtIsNull(clientId);

            AccountEntity newAccountEntity = new AccountEntity();
            newAccountEntity.setAccountId(newAccountId);
            newAccountEntity.setName(request.getName());
            newAccountEntity.setUsername(request.getUsername());
            newAccountEntity.setPassword(hashedPassword);
            newAccountEntity.setRole(request.getRole());
            newAccountEntity.setClientEntity(clientEntity);
            newAccountEntity.setCreatedAt(getCurrentTimestamp());
            newAccountEntity.setUpdatedAt(getCurrentTimestamp());
            accountRepository.save(newAccountEntity);
            return true;
        }catch (Exception e){
            return false;
        }

    }

    public boolean doUpdateAccount(EditUserRequest request){
        AccountEntity accountEntity = accountRepository.findByAccountIdAndDeletedAtIsNull(request.getId());
        if(accountEntity != null){
            System.out.println("Account found");
            accountEntity.setUsername(request.getUsername());
            accountEntity.setPassword(request.getPassword());
            accountEntity.setUsername(request.getUsername());
            accountEntity.setRole(request.getRole());
            accountRepository.save(accountEntity);
            return true;
        }else{
            System.out.println("Account Not Found");
            return false;
        }
    }

    public List<UserDTO> getUserList(String clientId){
        List<AccountEntity> accountEntities = accountRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNull(clientId);
        return accountEntities.stream().map(user -> new UserDTO(
                user.getAccountId(),
                user.getName(),
                user.getUsername(),
                user.getRole()
        )).toList();
    }

    public boolean doDisableAccount(String userId){
        AccountEntity accountEntity = accountRepository.findByAccountIdAndDeletedAtIsNull(userId);
        if(accountEntity == null){
            System.out.println("Account Not Found");
            return false;
        }

        accountEntity.setDeletedAt(getCurrentTimestamp());
        accountRepository.save(accountEntity);
        return true;
    }
}
