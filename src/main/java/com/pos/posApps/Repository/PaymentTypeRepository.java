package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PaymentTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTypeRepository extends JpaRepository<PaymentTypeEntity, Long> {
    List<PaymentTypeEntity> findByClientEntity_ClientIdAndDeletedAtIsNullOrderByPaymentTypeIdAsc(
            Long clientId
    );

    Optional<PaymentTypeEntity> findByPaymentTypeIdAndClientEntity_ClientIdAndDeletedAtIsNull(
            Long paymentTypeId,
            Long clientId
    );

    Optional<PaymentTypeEntity> findFirstByClientEntity_ClientIdAndBehaviorAndDeletedAtIsNullOrderByPaymentTypeIdAsc(
            Long clientId,
            String behavior
    );

    boolean existsByClientEntity_ClientIdAndNameIgnoreCaseAndDeletedAtIsNull(
            Long clientId,
            String name
    );

    boolean existsByClientEntity_ClientIdAndNameIgnoreCaseAndDeletedAtIsNullAndPaymentTypeIdNot(
            Long clientId,
            String name,
            Long paymentTypeId
    );
}
