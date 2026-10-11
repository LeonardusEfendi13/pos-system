package com.pos.posApps.Repository;

import com.pos.posApps.Entity.StagingPriceListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StagingPriceListRepository extends JpaRepository<StagingPriceListEntity, Long> {
    List<StagingPriceListEntity> findAllByMerkOrderByExcelRowAsc(String merk);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StagingPriceListEntity item where item.merk = :merk")
    int deleteByMerk(@Param("merk") String merk);
}
