package com.pos.posApps.Repository;

import com.pos.posApps.Entity.AccountSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AccountSettingRepository extends JpaRepository<AccountSettingEntity, Long> {

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO account_setting (account_id, font_weight_level)
            VALUES (:accountId, :level)
            ON CONFLICT (account_id)
            DO UPDATE SET font_weight_level = EXCLUDED.font_weight_level, updated_at = NOW()
            """, nativeQuery = true)
    void upsertFontWeightLevel(@Param("accountId") Long accountId, @Param("level") int level);
}
