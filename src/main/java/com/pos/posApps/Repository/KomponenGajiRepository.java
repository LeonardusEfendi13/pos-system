package com.pos.posApps.Repository;

import com.pos.posApps.Entity.KomponenGajiEntity;
import com.pos.posApps.Entity.PayrollDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KomponenGajiRepository extends JpaRepository<KomponenGajiEntity, Long> {
    List<KomponenGajiEntity> findByIsActiveAndDeletedAtIsNull(Boolean isActive);
}
