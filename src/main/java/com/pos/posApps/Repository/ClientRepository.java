package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<ClientEntity, String> {
    ClientEntity findByClientId(String clientId);

    ClientEntity findFirstByOrderByClientIdDesc();


}
