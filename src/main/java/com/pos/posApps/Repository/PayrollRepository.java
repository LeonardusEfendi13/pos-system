package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PayrollEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<PayrollEntity, Long> {
    List<PayrollEntity> findByClientEntity_ClientIdAndPayrollYearAndPayrollMonthAndDeletedAtIsNull(
            Long clientId,
            int payrollYear,
            int payrollMonth
    );

    Optional<PayrollEntity> findFirstByClientEntity_ClientIdAndStaffEntity_StaffIdAndPayrollYearAndPayrollMonthAndDeletedAtIsNull(
            Long clientId,
            Long staffId,
            int payrollYear,
            int payrollMonth
    );
}
