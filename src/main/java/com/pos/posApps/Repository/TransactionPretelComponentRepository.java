package com.pos.posApps.Repository;

import com.pos.posApps.Entity.TransactionPretelComponentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionPretelComponentRepository extends JpaRepository<TransactionPretelComponentEntity, Long> {
}
