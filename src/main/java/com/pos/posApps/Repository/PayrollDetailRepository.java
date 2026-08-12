package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PayrollDetailEntity;
import com.pos.posApps.Entity.PayrollEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollDetailRepository extends JpaRepository<PayrollDetailEntity, Long> {
}
