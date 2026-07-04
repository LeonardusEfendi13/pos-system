package com.pos.posApps.Repository;

import com.pos.posApps.Entity.StaffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<StaffEntity, Long> {
    List<StaffEntity> findAllByDeletedAtIsNull();

    StaffEntity findFirstByStaffIdAndDeletedAtIsNull(Long staffId);
}
