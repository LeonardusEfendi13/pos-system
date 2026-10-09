package com.pos.posApps.Service;

import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Util.MenuCatalog;

import java.util.List;

public record EffectiveAccess(
        Roles role,
        String roleName,
        List<String> menuKeys,
        boolean godAdmin,
        boolean superAdmin
) {
    public boolean allows(String key) {
        if (superAdmin) {
            return true;
        }
        return key != null && menuKeys.contains(key);
    }

    public boolean allowsAny(List<String> keys) {
        if (superAdmin) {
            return true;
        }
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        return keys.stream().anyMatch(menuKeys::contains);
    }

    public String landingPath() {
        return MenuCatalog.landingPath(godAdmin, superAdmin, menuKeys);
    }
}
