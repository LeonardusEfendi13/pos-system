package com.pos.posApps.Repository;

import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, Long> {
    List<BranchEntity> findAllByDeletedAtIsNullOrderByBranchIdDesc();

    Optional<BranchEntity> findAllByBranchIdAndDeletedAtIsNull(Long id);

}
