package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreorderDetailRepository extends JpaRepository<PreorderDetailEntity, Long> {
    void deleteAllByPreorderEntity_PreorderId(Long preorderId);
}
