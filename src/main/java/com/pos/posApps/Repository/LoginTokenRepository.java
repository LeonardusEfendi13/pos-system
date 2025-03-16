package com.pos.posApps.Repository;

import com.pos.posApps.Entity.LoginTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginTokenRepository extends JpaRepository<LoginTokenEntity, String> {
}
