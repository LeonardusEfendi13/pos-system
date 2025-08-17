package com.pos.posApps.Repository;

import com.pos.posApps.Entity.LoginTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginTokenRepository extends JpaRepository<LoginTokenEntity, String> {

    Optional<LoginTokenEntity> findFirstByOrderByTokenIdDesc();

    LoginTokenEntity findByToken(String token);
}
