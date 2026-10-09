package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "role_menu_grant")
@IdClass(RoleMenuGrantId.class)
public class RoleMenuGrantEntity {
    @Id
    @Column(name = "client_role_id")
    private Long clientRoleId;

    @Id
    @Column(name = "menu_key")
    private String menuKey;
}
