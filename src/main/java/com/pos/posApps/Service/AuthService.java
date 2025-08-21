package com.pos.posApps.Service;

import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.LoginTokenEntity;
import com.pos.posApps.Repository.AccountRepository;
import com.pos.posApps.Repository.LoginTokenRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class AuthService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoginTokenRepository loginTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String doLoginAndGetToken(String username, String password) {
        try {
            AccountEntity accountData = accountRepository.findByUsernameAndDeletedAtIsNull(username);
            if (accountData == null) {
                System.out.println("Account not found");
                return null;
            }

            boolean isPasswordEqual = passwordEncoder.matches(password, accountData.getPassword());
            if(!isPasswordEqual) {
                return null;
            }

            //Generate login token
            String generatedToken = Generator.generateToken();

            //Get last token id
            String lastTokenId = loginTokenRepository.findFirstByOrderByCreatedAtDesc().map(LoginTokenEntity::getTokenId).orElse("LTN0");
            String newToken = Generator.generateId(lastTokenId);


            //Update Account Data
            accountData.setLastLogin(getCurrentTimestamp());
            accountData.setUpdatedAt(getCurrentTimestamp());
            accountRepository.save(accountData);

            //Insert into LoginToken table
            LoginTokenEntity loginToken = new LoginTokenEntity();
            loginToken.setTokenId(newToken);
            loginToken.setToken(generatedToken);
            loginToken.setCreatedAt(getCurrentTimestamp());
            loginToken.setAccountEntity(accountData);
            loginTokenRepository.save(loginToken);
            return generatedToken;
        } catch (Exception e) {
            System.out.println("Error in login");
            e.printStackTrace();
            return null;
        }
    }

    public AccountEntity validateToken(String token) {
        LoginTokenEntity loginTokenEntity = loginTokenRepository.findByTokenAndDeletedAtIsNull(token);
        if(loginTokenEntity == null) {
            return null;
        }
        return loginTokenEntity.getAccountEntity();
    }

    public boolean hasAccessToModifyData(Roles role){
        return (role.equals(Roles.SUPER_ADMIN) || role.equals(Roles.GOD_ADMIN));
    }


}
