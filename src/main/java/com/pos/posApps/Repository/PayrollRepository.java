package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PayrollEntity;
import com.pos.posApps.Entity.StaffEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<PayrollEntity, Long> {

    List<PayrollEntity> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);
}
