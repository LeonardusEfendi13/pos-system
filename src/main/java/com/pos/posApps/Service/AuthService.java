package com.pos.posApps.Service;

import com.pos.posApps.DTO.RegisterDTO.RegisterRequest;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.LoginTokenEntity;
import com.pos.posApps.Repository.AccountRepository;
import com.pos.posApps.Repository.LoginTokenRepository;
import com.pos.posApps.Util.Generator;
import com.pos.posApps.Util.Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoginTokenRepository loginTokenRepository;

    public String doLoginAndGetToken(String username, String password) {
        try {

            AccountEntity accountData = accountRepository.findByUsername(username);
            if (accountData == null) {
                System.out.println("Account not found");
                return null;
            }

            //Encrypt password input with salt from db
            String encryptedPassword = Hash.hashPassword(password, Hash.stringToSalt(accountData.getSalt()));

            //Validate password
            if(!encryptedPassword.equals(accountData.getPassword())) {
                return null;
            }

            //Generate login token
            String generatedToken = Generator.generateToken();

            //Get last token id
            String lastTokenId = loginTokenRepository.findFirstByOrderByTokenIdDesc().getTokenId();
            String newToken;

            //If no data, then the id is 0, otherwise get last id from db
            if(lastTokenId == null) {
                newToken = Generator.generateId("LTN0");
            }else{
                newToken = Generator.generateId(lastTokenId);
            }

            //Insert into LoginToken table
            LoginTokenEntity loginToken = new LoginTokenEntity();
            loginToken.setTokenId(newToken);
            loginToken.setToken(generatedToken);
            loginToken.setCreatedAt(Generator.getCurrentTimestamp());
            loginToken.setAccountEntity(accountData);
            loginTokenRepository.save(loginToken);

            return generatedToken;
        } catch (Exception e) {
            System.out.println("Error in encryption");
            e.printStackTrace();
            return null;
        }
    }

    public String validateToken(String token) {
        LoginTokenEntity loginTokenEntity = loginTokenRepository.findByToken(token);
        if(loginTokenEntity == null) {
            return null;
        }
//        AccountEntity accountEntity = loginTokenEntity.getAccountEntity();
        System.out.println("account entity : " + loginTokenEntity.getAccountEntity());
        return loginTokenEntity.getAccountEntity().getClientEntity().getClientId();
    }

    public boolean doCreateAccount(RegisterRequest request, String clientId){

        return true;
    }
}
