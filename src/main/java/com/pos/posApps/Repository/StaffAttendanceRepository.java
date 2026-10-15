package com.pos.posApps.Repository;

import com.pos.posApps.Entity.StaffAttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendanceEntity, Long> {
    List<StaffAttendanceEntity> findByStaffEntity_ClientEntity_ClientIdAndAttendanceDateBetween(
            Long clientId,
            LocalDate start,
            LocalDate end
    );

    Optional<StaffAttendanceEntity> findByStaffEntity_StaffIdAndAttendanceDate(Long staffId, LocalDate attendanceDate);
}
