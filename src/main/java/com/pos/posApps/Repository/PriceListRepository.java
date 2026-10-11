package com.pos.posApps.Repository;

import com.pos.posApps.Entity.PriceListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriceListRepository extends JpaRepository<PriceListEntity, String> {
    Optional<PriceListEntity> findAllByPartNumber(String partNumber);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PriceListEntity item where upper(trim(item.merk)) = :merk")
    int deleteByNormalizedMerk(@Param("merk") String merk);
}
