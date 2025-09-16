package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PreorderDetailRepository extends JpaRepository<PreorderDetailEntity, Long> {
    Optional<PreorderDetailEntity> findFirstByDeletedAtIsNullOrderByPreorderDetailIdDesc();
    void deleteAllByPreorderEntity_PreorderId(Long preorderId);
}
