package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PriceListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriceListRepository extends JpaRepository<PriceListEntity, String> {
    Optional<PriceListEntity> findAllByPartNumber(String partNumber);
}
