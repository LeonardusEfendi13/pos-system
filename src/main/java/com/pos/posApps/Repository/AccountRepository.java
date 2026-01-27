package com.pos.posApps.Repository;

import com.pos.posApps.Entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    AccountEntity findByUsernameAndDeletedAtIsNull(String username);

    List<AccountEntity>  findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByAccountIdDesc(Long clientId);

    AccountEntity findByAccountIdAndDeletedAtIsNull(Long id);
}
