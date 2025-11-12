package com.pos.posApps.Service;

import com.pos.posApps.Entity.LoginTokenEntity;
import com.pos.posApps.Repository.LoginTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Optional;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class LoginTokenService {
    @Autowired
    private LoginTokenRepository loginTokenRepository;

    @Transactional
    public boolean deleteToken(String token){
        try{
            Optional<LoginTokenEntity> loginTokenEntityOptional = loginTokenRepository.findByTokenAndDeletedAtIsNull(token);
            if(loginTokenEntityOptional.isEmpty()){
                return false;
            }

            LoginTokenEntity loginTokenEntity = loginTokenEntityOptional.get();
            loginTokenEntity.setDeletedAt(getCurrentTimestamp());
            loginTokenRepository.save(loginTokenEntity);
            return true;
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }
}
