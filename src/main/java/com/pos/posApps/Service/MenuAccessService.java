package com.pos.posApps.Service;

import com.pos.posApps.Entity.AccountEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuAccessService {
    private final AccessResolver accessResolver;

    public MenuAccessService(AccessResolver accessResolver) {
        this.accessResolver = accessResolver;
    }

    public EffectiveAccess resolve(AccountEntity account) {
        return accessResolver.resolve(account);
    }

    public boolean allows(AccountEntity account, String menuKey) {
        EffectiveAccess access = accessResolver.resolve(account);
        return access != null && access.allows(menuKey);
    }

    public boolean allowsAny(AccountEntity account, List<String> menuKeys) {
        EffectiveAccess access = accessResolver.resolve(account);
        return access != null && access.allowsAny(menuKeys);
    }
}
