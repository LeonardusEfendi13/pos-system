package com.pos.posApps.Repository;

import com.pos.posApps.Entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {
    CustomerEntity findByNameAndDeletedAtIsNullAndClientEntity_ClientId(String name, String clientId);

    List<CustomerEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByCustomerIdAsc(String clientId);

    Optional<CustomerEntity> findFirstByOrderByCustomerIdDesc();

    CustomerEntity findByCustomerIdAndDeletedAtIsNull(String customerId);
}
