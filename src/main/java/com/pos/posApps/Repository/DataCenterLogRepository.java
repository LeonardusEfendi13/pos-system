package com.pos.posApps.Repository;

import com.pos.posApps.Entity.DataCenterLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface DataCenterLogRepository extends JpaRepository<DataCenterLogEntity, LocalDateTime> {
}
