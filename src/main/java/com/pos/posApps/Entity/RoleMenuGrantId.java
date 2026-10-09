package com.pos.posApps.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuGrantId implements Serializable {
    private Long clientRoleId;
    private String menuKey;
}
