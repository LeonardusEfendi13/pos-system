package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderDetailEntity;
import com.pos.posApps.Entity.PreorderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreorderDetailRepository extends JpaRepository<PreorderDetailEntity, String> {
    Optional<PreorderDetailEntity> findFirstByOrderByPreorderDetailIdDesc();

    PreorderDetailEntity findFirstByPreorderDetailId(String preorderDetailId);

    List<PreorderDetailEntity> findAllByPreorderEntity_PreorderIdOrderByPreorderDetailIdAsc(String preorderId);
}
