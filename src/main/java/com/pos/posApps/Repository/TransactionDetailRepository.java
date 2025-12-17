package com.pos.posApps.Repository;

import com.pos.posApps.DTO.Dtos.Home.HomeProductDTO;
import com.pos.posApps.Entity.TransactionDetailEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetailEntity, Long> {
    List<TransactionDetailEntity> findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionDetailIdDesc(Long transactionId);
    void deleteAllByTransactionEntity_TransactionId(Long transactionId);

    @Query("""
    SELECT new com.pos.posApps.DTO.Dtos.Home.HomeProductDTO(
        p.fullName,
        SUM(td.qty)
    )
    FROM TransactionDetailEntity td
    JOIN ProductEntity p ON p.shortName = td.shortName
    WHERE td.deletedAt IS NULL
      AND p.deletedAt IS NULL
      AND td.createdAt BETWEEN :startDate AND :endDate
    GROUP BY p.fullName
    ORDER BY SUM(td.qty) DESC
""")
    List<HomeProductDTO> findTopProducts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
