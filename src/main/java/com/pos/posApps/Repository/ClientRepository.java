package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<ClientEntity, Long> {
    ClientEntity findByClientIdAndDeletedAtIsNull(Long clientId);

    Optional<ClientEntity> findFirstByOrderByClientIdDesc();


}
