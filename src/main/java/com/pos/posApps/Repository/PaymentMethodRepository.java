package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Long> {
    List<PaymentMethodEntity> findByClientEntity_ClientIdAndDeletedAtIsNullOrderByPaymentMethodIdAsc(
            Long clientId
    );

    Optional<PaymentMethodEntity> findByPaymentMethodIdAndClientEntity_ClientIdAndDeletedAtIsNull(
            Long paymentMethodId,
            Long clientId
    );

    Optional<PaymentMethodEntity> findFirstByClientEntity_ClientIdAndSystemDefaultTrueAndDeletedAtIsNull(
            Long clientId
    );
}
