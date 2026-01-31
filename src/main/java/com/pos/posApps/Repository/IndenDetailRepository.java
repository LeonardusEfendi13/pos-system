package com.pos.posApps.Repository;

import com.pos.posApps.Entity.IndenDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndenDetailRepository extends JpaRepository<IndenDetailEntity, Long> {
    void deleteAllByIndenEntity_IndenId(Long indenId);

    List<IndenDetailEntity> findAllByIndenEntity_IndenIdAndDeletedAtIsNullOrderByIndenDetailIdDesc(Long indenId);

}
