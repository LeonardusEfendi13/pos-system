package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.CategoryDTO;
import com.pos.posApps.DTO.Dtos.CreateCategoryRequest;
import com.pos.posApps.DTO.Dtos.EditCategoryRequest;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.CategoryEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Repository.CategoryRepository;
import com.pos.posApps.Repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public CategoryDTO toDto(CategoryEntity entity) {
        CategoryEntity parent = entity.getParent();
        return new CategoryDTO(
                entity.getCategoryId(),
                entity.getName(),
                parent == null ? null : parent.getCategoryId(),
                parent == null ? null : parent.getName()
        );
    }

    public List<CategoryDTO> listAll(Long clientId) {
        return categoryRepository
                .findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByNameAsc(clientId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Page<CategoryDTO> search(Long clientId, String search, Pageable pageable) {
        String trimmed = search == null ? "" : search.trim();
        return categoryRepository.searchByClientId(clientId, trimmed, pageable).map(this::toDto);
    }

    public Optional<CategoryEntity> findActive(Long categoryId, Long clientId) {
        if (categoryId == null) {
            return Optional.empty();
        }
        return categoryRepository.findFirstByCategoryIdAndClientEntity_ClientIdAndDeletedAtIsNull(categoryId, clientId);
    }

    @Transactional
    public ResponseInBoolean insert(CreateCategoryRequest req, ClientEntity clientData) {
        try {
            String name = normalizeName(req.getName());
            if (name.isEmpty()) {
                return new ResponseInBoolean(false, "Nama kategori wajib diisi");
            }

            CategoryEntity parent = null;
            if (req.getParentId() != null) {
                Optional<CategoryEntity> parentOpt = findActive(req.getParentId(), clientData.getClientId());
                if (parentOpt.isEmpty()) {
                    return new ResponseInBoolean(false, "Kategori induk tidak ditemukan");
                }
                parent = parentOpt.get();
                if (parent.getParent() != null) {
                    return new ResponseInBoolean(false, "Sub kategori tidak boleh memiliki anak");
                }
            }

            if (isDuplicateName(name, clientData.getClientId(), req.getParentId(), null)) {
                return new ResponseInBoolean(false, "Nama kategori sudah ada");
            }

            CategoryEntity entity = new CategoryEntity();
            entity.setName(name);
            entity.setParent(parent);
            entity.setClientEntity(clientData);
            categoryRepository.save(entity);
            return new ResponseInBoolean(true, "Berhasil tambah kategori");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Gagal tambah kategori");
        }
    }

    @Transactional
    public ResponseInBoolean edit(EditCategoryRequest req, ClientEntity clientData) {
        try {
            if (req.getCategoryId() == null) {
                return new ResponseInBoolean(false, "Kategori tidak ditemukan");
            }

            Optional<CategoryEntity> entityOpt = findActive(req.getCategoryId(), clientData.getClientId());
            if (entityOpt.isEmpty()) {
                return new ResponseInBoolean(false, "Kategori tidak ditemukan");
            }

            CategoryEntity entity = entityOpt.get();
            String name = normalizeName(req.getName());
            if (name.isEmpty()) {
                return new ResponseInBoolean(false, "Nama kategori wajib diisi");
            }

            CategoryEntity parent = null;
            if (req.getParentId() != null) {
                if (req.getParentId().equals(req.getCategoryId())) {
                    return new ResponseInBoolean(false, "Kategori tidak boleh menjadi induk dirinya sendiri");
                }
                Optional<CategoryEntity> parentOpt = findActive(req.getParentId(), clientData.getClientId());
                if (parentOpt.isEmpty()) {
                    return new ResponseInBoolean(false, "Kategori induk tidak ditemukan");
                }
                parent = parentOpt.get();
                if (parent.getParent() != null) {
                    return new ResponseInBoolean(false, "Sub kategori tidak boleh memiliki anak");
                }
                if (categoryRepository.existsByParent_CategoryIdAndDeletedAtIsNull(entity.getCategoryId())) {
                    return new ResponseInBoolean(false, "Kategori yang punya sub tidak bisa diubah menjadi sub");
                }
            }

            if (isDuplicateName(name, clientData.getClientId(), req.getParentId(), req.getCategoryId())) {
                return new ResponseInBoolean(false, "Nama kategori sudah ada");
            }

            entity.setName(name);
            entity.setParent(parent);
            categoryRepository.save(entity);
            return new ResponseInBoolean(true, "Berhasil ubah kategori");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Gagal ubah kategori");
        }
    }

    @Transactional
    public ResponseInBoolean delete(Long categoryId, ClientEntity clientData) {
        try {
            Optional<CategoryEntity> entityOpt = findActive(categoryId, clientData.getClientId());
            if (entityOpt.isEmpty()) {
                return new ResponseInBoolean(false, "Kategori tidak ditemukan");
            }

            if (categoryRepository.existsByParent_CategoryIdAndDeletedAtIsNull(categoryId)) {
                return new ResponseInBoolean(false, "Hapus sub kategori terlebih dahulu");
            }

            if (productRepository.existsByCategoryEntity_CategoryIdAndDeletedAtIsNull(categoryId)) {
                return new ResponseInBoolean(false, "Kategori masih dipakai barang");
            }

            CategoryEntity entity = entityOpt.get();
            entity.setDeletedAt(getCurrentTimestamp());
            categoryRepository.save(entity);
            return new ResponseInBoolean(true, "Berhasil hapus kategori");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Gagal hapus kategori");
        }
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private boolean isDuplicateName(String name, Long clientId, Long parentId, Long excludeId) {
        long excluded = excludeId == null ? -1L : excludeId;
        if (parentId == null) {
            return categoryRepository
                    .existsByNameIgnoreCaseAndClientEntity_ClientIdAndDeletedAtIsNullAndParentIsNullAndCategoryIdNot(
                            name, clientId, excluded);
        }
        return categoryRepository
                .existsByNameIgnoreCaseAndClientEntity_ClientIdAndDeletedAtIsNullAndParent_CategoryIdAndCategoryIdNot(
                        name, clientId, parentId, excluded);
    }
}
