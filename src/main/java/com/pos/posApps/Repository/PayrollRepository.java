package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PayrollEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollRepository extends JpaRepository<PayrollEntity, Long> {
    List<PayrollEntity> findByClientEntity_ClientIdAndPayrollYearAndPayrollMonthAndDeletedAtIsNull(
            Long clientId,
            int payrollYear,
            int payrollMonth
    );
}
