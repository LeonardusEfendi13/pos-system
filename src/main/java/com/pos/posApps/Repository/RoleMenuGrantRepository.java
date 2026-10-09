package com.pos.posApps.Repository;

import com.pos.posApps.Entity.RoleMenuGrantEntity;
import com.pos.posApps.Entity.RoleMenuGrantId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleMenuGrantRepository extends JpaRepository<RoleMenuGrantEntity, RoleMenuGrantId> {
    List<RoleMenuGrantEntity> findByClientRoleId(Long clientRoleId);

    void deleteByClientRoleId(Long clientRoleId);
}
