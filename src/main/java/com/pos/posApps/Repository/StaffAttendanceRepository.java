package com.pos.posApps.Repository;

import com.pos.posApps.Entity.StaffAttendanceEntity;
import com.pos.posApps.Entity.StaffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendanceEntity, Long> {
    List<StaffAttendanceEntity> findByAttendanceDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    Optional<StaffAttendanceEntity> findByStaffEntityAndAttendanceDateBetween(StaffEntity staffEntity, LocalDateTime startDate, LocalDateTime endDate);

}
