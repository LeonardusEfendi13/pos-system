package com.pos.posApps.Repository;

import com.pos.posApps.DTO.Enum.JenisTransaksiInsentive;
import com.pos.posApps.Entity.InsentiveLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface InsentiveLogRepository extends JpaRepository<InsentiveLogEntity, Long> {
    @Query("""
        SELECT COALESCE(SUM(i.nominal), 0) 
        FROM InsentiveLogEntity i 
        WHERE i.staffEntity.staffId = :staffId 
          AND i.jenisTransaksiInsentive = :jenisTransaksi
          AND i.deletedAt IS NULL
    """)
    BigDecimal sumNominalByStaffIdAndJenis(
            @Param("staffId") Long staffId,
            @Param("jenisTransaksi") JenisTransaksiInsentive jenisTransaksi
    );
}
