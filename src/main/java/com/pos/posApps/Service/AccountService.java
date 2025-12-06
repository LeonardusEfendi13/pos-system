package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.EditUserRequest;
import com.pos.posApps.DTO.Dtos.RegisterRequest;
import com.pos.posApps.DTO.Dtos.UserDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.LoginTokenEntity;
import com.pos.posApps.Repository.AccountRepository;
import com.pos.posApps.Repository.ClientRepository;
import com.pos.posApps.Repository.LoginTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class AccountService {
    @Autowired
    AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LoginTokenRepository loginTokenRepository;

    @Transactional
    public boolean doCreateAccount(RegisterRequest request, ClientEntity clientData){
        try{
            AccountEntity accountEntity = accountRepository.findByUsernameAndDeletedAtIsNull(request.getUsername());
            if(accountEntity != null){
                return false;
            }
            String hashedPassword = passwordEncoder.encode(request.getPassword());
//            Long lastAccountId = accountRepository.findFirstByOrderByAccountIdDesc().map(AccountEntity::getAccountId).orElse(0L);
//            Long newAccountId = Generator.generateId(lastAccountId);

            AccountEntity newAccountEntity = new AccountEntity();
//            newAccountEntity.setAccountId(newAccountId);
            newAccountEntity.setName(request.getName());
            newAccountEntity.setUsername(request.getUsername());
            newAccountEntity.setPassword(hashedPassword);
            newAccountEntity.setRole(request.getRole());
            newAccountEntity.setClientEntity(clientData);
            newAccountEntity.setCreatedAt(getCurrentTimestamp());
            newAccountEntity.setUpdatedAt(getCurrentTimestamp());
            accountRepository.save(newAccountEntity);
            return true;
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

    }

    public boolean doUpdateAccount(EditUserRequest request){
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

    public List<UserDTO> getUserList(Long clientId){
        List<AccountEntity> accountEntities = accountRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByAccountIdDesc(clientId);
        return accountEntities.stream().map(user -> new UserDTO(
                user.getAccountId(),
                user.getName(),
                user.getUsername(),
                user.getRole()
        )).toList();
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

    public boolean doDisableAccount(Long userId){
        AccountEntity accountEntity = accountRepository.findByAccountIdAndDeletedAtIsNull(userId);
        if(accountEntity == null){
            return false;
        }

        accountEntity.setDeletedAt(getCurrentTimestamp());
        accountRepository.save(accountEntity);
        return true;
    }
}
