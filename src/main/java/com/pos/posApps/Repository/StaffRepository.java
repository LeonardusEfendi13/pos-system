package com.pos.posApps.Repository;

import com.pos.posApps.Entity.StaffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<StaffEntity, Long> {
    List<StaffEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNull(Long clientId);

    Optional<StaffEntity> findByStaffIdAndDeletedAtIsNullAndClientEntity_ClientId(Long staffId, Long clientId);

    Optional<StaffEntity> findByClientEntity_ClientIdAndNikIgnoreCaseAndDeletedAtIsNull(Long clientId, String nik);

    boolean existsByClientEntity_ClientIdAndNikIgnoreCaseAndDeletedAtIsNullAndStaffIdNot(
            Long clientId,
            String nik,
            Long staffId
    );
}
