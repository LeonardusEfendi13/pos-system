package com.pos.posApps.Repository;

import com.pos.posApps.Entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    CustomerEntity findByNameAndDeletedAtIsNullAndClientEntity_ClientId(String name, Long clientId);

    Optional<CustomerEntity> findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(Long customerId, Long clientId);

    List<CustomerEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByCustomerIdDesc(Long clientId);

    Optional<CustomerEntity> findFirstByOrderByCustomerIdDesc();
}
