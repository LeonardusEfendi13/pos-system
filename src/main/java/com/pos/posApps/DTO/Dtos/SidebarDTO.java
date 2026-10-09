package com.pos.posApps.DTO.Dtos;

import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SidebarDTO {
    private String namaToko;
    private Long accountId;
    private String accountName;
    private Roles role;
    private String roleName;
    private List<String> menuKeys;
}
