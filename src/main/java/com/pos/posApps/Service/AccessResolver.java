package com.pos.posApps.Service;

import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientRoleEntity;
import com.pos.posApps.Entity.RoleMenuGrantEntity;
import com.pos.posApps.Repository.RoleMenuGrantRepository;
import com.pos.posApps.Util.MenuCatalog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessResolver {
    private final GodAdminCredentials godAdminCredentials;
    private final RoleMenuGrantRepository roleMenuGrantRepository;

    public AccessResolver(
            GodAdminCredentials godAdminCredentials,
            RoleMenuGrantRepository roleMenuGrantRepository
    ) {
        this.godAdminCredentials = godAdminCredentials;
        this.roleMenuGrantRepository = roleMenuGrantRepository;
    }

    public EffectiveAccess resolve(AccountEntity account) {
        if (account == null || account.getRole() == null) {
            return null;
        }

        if (godAdminCredentials.matchesUsername(account.getUsername())) {
            return new EffectiveAccess(
                    Roles.GOD_ADMIN,
                    "God Admin",
                    List.of(MenuCatalog.USER),
                    true,
                    false
            );
        }

        if (account.getRole() == Roles.SUPER_ADMIN) {
            return new EffectiveAccess(
                    Roles.SUPER_ADMIN,
                    "Super Admin",
                    MenuCatalog.allKeys(),
                    false,
                    true
            );
        }

        ClientRoleEntity clientRole = account.getClientRole();
        if (clientRole == null) {
            return new EffectiveAccess(
                    account.getRole(),
                    legacyName(account.getRole()),
                    MenuCatalog.STARTER_KEYS,
                    false,
                    false
            );
        }

        List<String> keys = roleMenuGrantRepository.findByClientRoleId(clientRole.getClientRoleId()).stream()
                .map(RoleMenuGrantEntity::getMenuKey)
                .filter(MenuCatalog::isKnownKey)
                .distinct()
                .toList();

        return new EffectiveAccess(
                account.getRole(),
                clientRole.getName(),
                keys,
                false,
                false
        );
    }

    private String legacyName(Roles role) {
        return switch (role) {
            case ADMIN_1 -> "ADMIN_1";
            case ADMIN_2 -> "ADMIN_2";
            case ADMIN_3 -> "ADMIN_3";
            case SUPER_ADMIN -> "Super Admin";
            case GOD_ADMIN -> "God Admin";
        };
    }
}
