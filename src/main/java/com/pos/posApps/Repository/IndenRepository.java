package com.pos.posApps.Repository;

import com.pos.posApps.Entity.IndenEntity;
import com.pos.posApps.Entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IndenRepository extends JpaRepository<IndenEntity, Long> {
    Page<IndenEntity> findAllByDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT t FROM IndenEntity t " +
            "WHERE t.indenDetailEntities IS NOT EMPTY " +
            "AND t.deletedAt IS NULL " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "AND (" +
            "    LOWER(t.indenNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.customerName) LIKE LOWER(CONCAT('%', :search, '%'))" +
            "    OR LOWER(t.customerPhone) LIKE LOWER(CONCAT('%', :search, '%'))" +
            ") " +
            "ORDER BY t.indenId DESC")
    Page<IndenEntity> searchIndens(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            Pageable pageable
    );

    Optional<IndenEntity> findFirstByIndenIdAndDeletedAtIsNull(Long transactionId);

}
