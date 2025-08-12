package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PreorderDetailEntity;
import com.pos.posApps.Entity.PreorderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreorderDetailRepository extends JpaRepository<PreorderDetailEntity, String> {
    PreorderDetailEntity findFirstByOrderByPreorderDetailIdDesc();

    PreorderDetailEntity findFirstByPreorderDetailId(String preorderDetailId);

    List<PreorderDetailEntity> findAllByPreorderEntity_PreorderIdOrderByPreorderDetailIdAsc(String preorderId);
}
