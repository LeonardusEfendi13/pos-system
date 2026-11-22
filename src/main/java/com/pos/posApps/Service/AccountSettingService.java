package com.pos.posApps.Service;

import com.pos.posApps.Repository.AccountSettingRepository;
import com.pos.posApps.Util.FontWeightLevel;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountSettingService {

    private AccountSettingRepository accountSettingRepository;

    public int getFontWeightLevel(Long accountId) {
        return accountSettingRepository.findById(accountId)
                .map(setting -> FontWeightLevel.orDefault(setting.getFontWeightLevel()))
                .orElse(FontWeightLevel.DEFAULT);
    }

    // Returns false when the level is outside 1-4; nothing is written then.
    public boolean updateFontWeightLevel(Long accountId, Integer level) {
        if (!FontWeightLevel.isValid(level)) {
            return false;
        }

        accountSettingRepository.upsertFontWeightLevel(accountId, level);
        return true;
    }
}
