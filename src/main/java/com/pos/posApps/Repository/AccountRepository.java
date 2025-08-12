package com.pos.posApps.Repository;

import com.pos.posApps.Entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, String> {
    AccountEntity findByUsernameAndDeletedAtIsNull(String username);

    AccountEntity findFirstByOrderByAccountIdDesc();

    List<AccountEntity> findAllByClientEntity_ClientId(String clientId);
    List<AccountEntity>  findAllByClientEntity_ClientIdAndDeletedAtIsNull(String clientId);

    AccountEntity findByAccountIdAndDeletedAtIsNull(String id);
}
