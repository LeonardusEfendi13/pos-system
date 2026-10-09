package com.pos.posApps.Repository;

import com.pos.posApps.Entity.ClientRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRoleRepository extends JpaRepository<ClientRoleEntity, Long> {
    List<ClientRoleEntity> findByClientIdOrderByNameAsc(Long clientId);

    Optional<ClientRoleEntity> findByClientRoleIdAndClientId(Long clientRoleId, Long clientId);

    boolean existsByClientIdAndNameIgnoreCase(Long clientId, String name);
}
