package com.pos.posApps.Repository;

import com.pos.posApps.Entity.InsentifPayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsentifPayoutRepository extends JpaRepository<InsentifPayoutEntity, Long> {
    List<InsentifPayoutEntity> findByClientEntity_ClientIdAndDeletedAtIsNullOrderByPaidAtDesc(Long clientId);
}
