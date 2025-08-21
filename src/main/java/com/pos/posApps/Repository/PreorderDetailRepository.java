package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderDetailEntity;
import com.pos.posApps.Entity.PreorderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreorderDetailRepository extends JpaRepository<PreorderDetailEntity, Long> {
    Optional<PreorderDetailEntity> findFirstByOrderByPreorderDetailIdDesc();

    PreorderDetailEntity findFirstByPreorderDetailId(Long preorderDetailId);

    List<PreorderDetailEntity> findAllByPreorderEntity_PreorderIdOrderByPreorderDetailIdDesc(Long preorderId);
}
