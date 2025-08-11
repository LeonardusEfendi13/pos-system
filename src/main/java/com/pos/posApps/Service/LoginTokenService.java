package com.pos.posApps.Service;

import com.pos.posApps.Entity.LoginTokenEntity;
import com.pos.posApps.Repository.LoginTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class LoginTokenService {
    @Autowired
    private LoginTokenRepository loginTokenRepository;

    @Transactional
    public boolean deleteToken(String token){
        try{
            LoginTokenEntity loginTokenEntity = loginTokenRepository.findByToken(token);
            if(loginTokenEntity == null){
                return false;
            }
            loginTokenEntity.setDeletedAt(getCurrentTimestamp());
            loginTokenRepository.save(loginTokenEntity);
            return true;
        }catch (Exception e){
            return false;
        }


    }
}
