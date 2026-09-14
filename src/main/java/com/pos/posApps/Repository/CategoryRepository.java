package com.pos.posApps.Repository;

import com.pos.posApps.Entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findFirstByCategoryIdAndClientEntity_ClientIdAndDeletedAtIsNull(
            Long categoryId,
            Long clientId
    );

    List<CategoryEntity> findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByNameAsc(Long clientId);

    boolean existsByParent_CategoryIdAndDeletedAtIsNull(Long parentId);

    boolean existsByNameIgnoreCaseAndClientEntity_ClientIdAndDeletedAtIsNullAndParent_CategoryIdAndCategoryIdNot(
            String name,
            Long clientId,
            Long parentId,
            Long categoryId
    );

    boolean existsByNameIgnoreCaseAndClientEntity_ClientIdAndDeletedAtIsNullAndParentIsNullAndCategoryIdNot(
            String name,
            Long clientId,
            Long categoryId
    );

    @Query("""
            SELECT c FROM CategoryEntity c
            LEFT JOIN c.parent p
            WHERE c.clientEntity.clientId = :clientId
              AND c.deletedAt IS NULL
              AND (
                    :search IS NULL
                    OR :search = ''
                    OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            """)
    Page<CategoryEntity> searchByClientId(
            @Param("clientId") Long clientId,
            @Param("search") String search,
            Pageable pageable
    );
}
