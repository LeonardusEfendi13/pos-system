package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.AdjustStockDTO;
import com.pos.posApps.DTO.Dtos.KasirPretelComponentDTO;
import com.pos.posApps.DTO.Dtos.KasirPretelDTO;
import com.pos.posApps.DTO.Dtos.PretelDraftDTO;
import com.pos.posApps.DTO.Dtos.PretelDraftLineDTO;
import com.pos.posApps.DTO.Dtos.ProductSetComponentDTO;
import com.pos.posApps.DTO.Dtos.ProductSetDTO;
import com.pos.posApps.DTO.Dtos.ProductSetPriceDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.SaveProductSetComponentRequest;
import com.pos.posApps.DTO.Dtos.SaveProductSetRequest;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.ProductEntity;
import com.pos.posApps.Entity.ProductPricesEntity;
import com.pos.posApps.Entity.ProductSetComponentEntity;
import com.pos.posApps.Entity.ProductSetEntity;
import com.pos.posApps.Entity.TransactionEntity;
import com.pos.posApps.Entity.TransactionPretelComponentEntity;
import com.pos.posApps.Entity.TransactionPretelEntity;
import com.pos.posApps.Repository.ProductRepository;
import com.pos.posApps.Repository.ProductSetComponentRepository;
import com.pos.posApps.Repository.ProductSetRepository;
import com.pos.posApps.Repository.TransactionPretelComponentRepository;
import com.pos.posApps.Repository.TransactionPretelRepository;
import com.pos.posApps.Util.PretelCount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class ProductSetService {
    private static final long NO_SET = -1L;

    private final ProductSetRepository productSetRepository;
    private final ProductSetComponentRepository productSetComponentRepository;
    private final ProductRepository productRepository;
    private final TransactionPretelRepository transactionPretelRepository;
    private final TransactionPretelComponentRepository transactionPretelComponentRepository;
    private final StockMovementService stockMovementService;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductSetService(
            ProductSetRepository productSetRepository,
            ProductSetComponentRepository productSetComponentRepository,
            ProductRepository productRepository,
            TransactionPretelRepository transactionPretelRepository,
            TransactionPretelComponentRepository transactionPretelComponentRepository,
            StockMovementService stockMovementService
    ) {
        this.productSetRepository = productSetRepository;
        this.productSetComponentRepository = productSetComponentRepository;
        this.productRepository = productRepository;
        this.transactionPretelRepository = transactionPretelRepository;
        this.transactionPretelComponentRepository = transactionPretelComponentRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional(readOnly = true)
    public Page<ProductSetDTO> search(Long clientId, String search, Pageable pageable) {
        String trimmed = search == null ? "" : search.trim();
        return productSetRepository.search(clientId, trimmed, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ProductSetDTO> findByParent(Long clientId, Long parentProductId) {
        return productSetRepository
                .findFirstByClientEntity_ClientIdAndParentProduct_ProductIdAndDeletedAtIsNull(clientId, parentProductId)
                .map(this::toDto);
    }

    @Transactional
    public ResponseInBoolean save(SaveProductSetRequest req, ClientEntity clientData) {
        try {
            if (req == null || req.getParentProductId() == null) {
                return fail("Barang induk wajib dipilih");
            }

            Long clientId = clientData.getClientId();
            Long excludeSetId = req.getProductSetId() == null ? NO_SET : req.getProductSetId();
            Optional<ProductEntity> parentOpt = activeProduct(req.getParentProductId(), clientId);
            if (parentOpt.isEmpty()) {
                return fail("Barang induk tidak ditemukan");
            }

            ProductEntity parent = parentOpt.get();
            if (productSetRepository
                    .existsByClientEntity_ClientIdAndParentProduct_ProductIdAndDeletedAtIsNullAndProductSetIdNot(
                            clientId,
                            parent.getProductId(),
                            excludeSetId
                    )) {
                return fail("Barang ini sudah punya barang set");
            }
            if (productSetComponentRepository.isActiveComponent(clientId, parent.getProductId(), excludeSetId)) {
                return fail("Barang induk sudah menjadi komponen barang set");
            }

            List<SaveProductSetComponentRequest> requested = req.getComponents() == null
                    ? List.of()
                    : req.getComponents();
            if (requested.isEmpty()) {
                return fail("Group set wajib punya minimal satu komponen");
            }

            Set<Long> seen = new HashSet<>();
            List<ProductSetComponentEntity> nextComponents = new ArrayList<>();
            for (SaveProductSetComponentRequest item : requested) {
                if (item == null || item.getProductId() == null) {
                    return fail("Barang komponen tidak ditemukan");
                }
                if (item.getQty() == null || item.getQty() == 0) {
                    return fail("Qty komponen tidak boleh 0");
                }
                if (!seen.add(item.getProductId())) {
                    return fail("Komponen yang sama tidak boleh dobel");
                }
                if (item.getProductId().equals(parent.getProductId())) {
                    return fail("Barang induk tidak boleh menjadi komponennya sendiri");
                }

                Optional<ProductEntity> childOpt = activeProduct(item.getProductId(), clientId);
                if (childOpt.isEmpty()) {
                    return fail("Barang komponen tidak ditemukan");
                }
                if (productSetRepository
                        .existsByClientEntity_ClientIdAndParentProduct_ProductIdAndDeletedAtIsNullAndProductSetIdNot(
                                clientId,
                                item.getProductId(),
                                excludeSetId
                        )) {
                    return fail("Barang anak sudah menjadi induk barang set");
                }

                ProductSetComponentEntity component = new ProductSetComponentEntity();
                component.setChildProduct(childOpt.get());
                component.setQty(item.getQty());
                nextComponents.add(component);
            }

            ProductSetEntity entity;
            if (req.getProductSetId() == null) {
                entity = new ProductSetEntity();
                entity.setClientEntity(clientData);
            } else {
                Optional<ProductSetEntity> existing = productSetRepository
                        .findFirstByProductSetIdAndClientEntity_ClientIdAndDeletedAtIsNull(
                                req.getProductSetId(),
                                clientId
                        );
                if (existing.isEmpty()) {
                    return fail("Group set tidak ditemukan");
                }
                entity = existing.get();
                retireComponents(entity);
            }

            entity.setParentProduct(parent);
            productSetRepository.save(entity);
            entityManager.flush();
            for (ProductSetComponentEntity component : nextComponents) {
                component.setProductSet(entity);
                productSetComponentRepository.save(component);
            }

            String message = req.getProductSetId() == null
                    ? "Berhasil tambah barang set"
                    : "Berhasil ubah barang set";
            return new ResponseInBoolean(true, message);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Gagal simpan barang set");
        }
    }

    @Transactional
    public ResponseInBoolean delete(Long productSetId, ClientEntity clientData) {
        try {
            Optional<ProductSetEntity> existing = productSetRepository
                    .findFirstByProductSetIdAndClientEntity_ClientIdAndDeletedAtIsNull(
                            productSetId,
                            clientData.getClientId()
                    );
            if (existing.isEmpty()) {
                return fail("Group set tidak ditemukan");
            }

            ProductSetEntity entity = existing.get();
            entity.setDeletedAt(getCurrentTimestamp());
            retireComponents(entity);
            productSetRepository.save(entity);
            return new ResponseInBoolean(true, "Berhasil hapus barang set");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Gagal hapus barang set");
        }
    }

    @Transactional
    public ResponseInBoolean applyForSale(
            List<PretelDraftDTO> drafts,
            TransactionEntity transaction,
            ClientEntity clientData,
            String notaNumber
    ) {
        if (drafts == null || drafts.isEmpty()) {
            return new ResponseInBoolean(true, "OK");
        }

        try {
            for (PretelDraftDTO draft : drafts) {
                ResponseInBoolean applied = applyDraft(draft, transaction, clientData, notaNumber);
                if (!applied.isStatus()) {
                    return applied;
                }
            }
            entityManager.flush();
            return new ResponseInBoolean(true, "OK");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Gagal mempretel barang set");
        }
    }

    // Re-assembles every set this sale pretelled. Never fails: stock may go negative
    @Transactional
    public void reverseForSale(TransactionEntity transaction, ClientEntity clientData) {
        for (TransactionPretelEntity pretel : activePretelEntities(transaction.getTransactionId())) {
            reverse(pretel, clientData, transaction.getTransactionNumber());
        }
        entityManager.flush();
    }

    // Edit: only parents whose pretel count changed are re-assembled, then pretelled again
    @Transactional
    public ResponseInBoolean syncForEdit(
            List<PretelDraftDTO> drafts,
            TransactionEntity transaction,
            ClientEntity clientData
    ) {
        try {
            List<TransactionPretelEntity> active = activePretelEntities(transaction.getTransactionId());
            Map<Long, Long> oldCounts = new HashMap<>();
            for (TransactionPretelEntity pretel : active) {
                oldCounts.merge(pretel.getParentProduct().getProductId(), pretel.getSetQty(), Long::sum);
            }

            Map<Long, PretelDraftDTO> draftByParent = new HashMap<>();
            Map<Long, Long> newCounts = new HashMap<>();
            for (PretelDraftDTO draft : drafts == null ? List.<PretelDraftDTO>of() : drafts) {
                if (draft == null || draft.getParentProductId() == null) {
                    return fail("Group set tidak ditemukan");
                }
                int count = draftCount(draft);
                if (count > 0) {
                    draftByParent.put(draft.getParentProductId(), draft);
                    newCounts.put(draft.getParentProductId(), (long) count);
                }
            }

            String notaNumber = transaction.getTransactionNumber();
            for (PretelCount.CountChange change : PretelCount.changedCounts(oldCounts, newCounts)) {
                for (TransactionPretelEntity pretel : active) {
                    if (pretel.getParentProduct().getProductId().equals(change.parentProductId())) {
                        reverse(pretel, clientData, notaNumber);
                    }
                }
                entityManager.flush();
                if (change.newCount() > 0) {
                    ResponseInBoolean applied = applyDraft(
                            draftByParent.get(change.parentProductId()),
                            transaction,
                            clientData,
                            notaNumber
                    );
                    if (!applied.isStatus()) {
                        return applied;
                    }
                }
            }
            entityManager.flush();
            return new ResponseInBoolean(true, "OK");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Gagal mempretel barang set");
        }
    }

    @Transactional(readOnly = true)
    public List<KasirPretelDTO> activePretels(Long transactionId) {
        List<KasirPretelDTO> result = new ArrayList<>();
        for (TransactionPretelEntity pretel : activePretelEntities(transactionId)) {
            long setQty = pretel.getSetQty() == null ? 0L : pretel.getSetQty();
            if (setQty <= 0) {
                continue;
            }
            List<KasirPretelComponentDTO> components = activeSnapshot(pretel).stream()
                    .map(component -> new KasirPretelComponentDTO(
                            component.getChildProduct().getProductId(),
                            component.getQtyIn() / setQty
                    ))
                    .toList();
            result.add(new KasirPretelDTO(
                    pretel.getParentProduct().getProductId(),
                    pretel.getParentProduct().getFullName(),
                    setQty,
                    components
            ));
        }
        return result;
    }

    private List<TransactionPretelEntity> activePretelEntities(Long transactionId) {
        return transactionPretelRepository
                .findAllByTransactionEntity_TransactionIdAndDeletedAtIsNullOrderByTransactionPretelIdAsc(transactionId);
    }

    private List<TransactionPretelComponentEntity> activeSnapshot(TransactionPretelEntity pretel) {
        if (pretel.getComponents() == null) {
            return List.of();
        }
        return pretel.getComponents().stream()
                .filter(component -> component.getDeletedAt() == null)
                .sorted(Comparator.comparing(component -> component.getChildProduct().getProductId()))
                .toList();
    }

    private int draftCount(PretelDraftDTO draft) {
        int clickCount = draft.getClickCount() == null ? 0 : draft.getClickCount();
        List<PretelCount.Line> lines = new ArrayList<>();
        for (PretelDraftLineDTO line : draft.getLines() == null ? List.<PretelDraftLineDTO>of() : draft.getLines()) {
            if (line == null || line.getQty() == null || line.getRecipeQty() == null) {
                continue;
            }
            lines.add(new PretelCount.Line(line.getQty(), line.getRecipeQty()));
        }
        return PretelCount.resolve(clickCount, lines);
    }

    // Undo one recorded pretel from its snapshot, not the current recipe
    private void reverse(TransactionPretelEntity pretel, ClientEntity clientData, String notaNumber) {
        Long clientId = clientData.getClientId();
        long setQty = pretel.getSetQty() == null ? 0L : pretel.getSetQty();
        Optional<ProductEntity> parentOpt = productRepository.findAndLockByProductIdIncludingDeleted(
                pretel.getParentProduct().getProductId(),
                clientId
        );

        for (TransactionPretelComponentEntity component : activeSnapshot(pretel)) {
            Optional<ProductEntity> childOpt = productRepository.findAndLockByProductIdIncludingDeleted(
                    component.getChildProduct().getProductId(),
                    clientId
            );
            long qtyOut = component.getQtyIn() == null ? 0L : component.getQtyIn();
            if (childOpt.isPresent() && qtyOut > 0) {
                ProductEntity child = childOpt.get();
                long nextStock = (child.getStock() == null ? 0L : child.getStock()) - qtyOut;
                child.setStock(nextStock);
                productRepository.save(child);
                stockMovementService.insertKartuStok(new AdjustStockDTO(
                        child,
                        notaNumber,
                        TipeKartuStok.KOREKSI_PRETEL,
                        0L,
                        qtyOut,
                        nextStock,
                        clientData,
                        getCurrentTimestamp()
                ));
            }
            component.setDeletedAt(getCurrentTimestamp());
            transactionPretelComponentRepository.save(component);
        }

        if (parentOpt.isPresent() && setQty > 0) {
            ProductEntity parent = parentOpt.get();
            long nextStock = (parent.getStock() == null ? 0L : parent.getStock()) + setQty;
            parent.setStock(nextStock);
            productRepository.save(parent);
            stockMovementService.insertKartuStok(new AdjustStockDTO(
                    parent,
                    notaNumber,
                    TipeKartuStok.KOREKSI_PRETEL,
                    setQty,
                    0L,
                    nextStock,
                    clientData,
                    getCurrentTimestamp()
            ));
        }

        pretel.setDeletedAt(getCurrentTimestamp());
        transactionPretelRepository.save(pretel);
    }

    private ResponseInBoolean applyDraft(
            PretelDraftDTO draft,
            TransactionEntity transaction,
            ClientEntity clientData,
            String notaNumber
    ) {
        if (draft == null || draft.getParentProductId() == null) {
            return fail("Group set tidak ditemukan");
        }

        int clickCount = draft.getClickCount() == null ? 0 : draft.getClickCount();
        List<PretelDraftLineDTO> lines = draft.getLines() == null ? List.of() : draft.getLines();
        if (clickCount <= 0 || lines.isEmpty()) {
            return new ResponseInBoolean(true, "OK");
        }

        Long clientId = clientData.getClientId();
        Optional<ProductSetEntity> setOpt = productSetRepository
                .findFirstByClientEntity_ClientIdAndParentProduct_ProductIdAndDeletedAtIsNull(
                        clientId,
                        draft.getParentProductId()
                );
        if (setOpt.isEmpty()) {
            return fail("Resep barang set tidak ditemukan");
        }

        List<ProductSetComponentEntity> recipe = activeComponents(setOpt.get());
        if (recipe.isEmpty()) {
            return fail("Resep barang set tidak ditemukan");
        }

        Map<Long, Long> recipeQty = new HashMap<>();
        for (ProductSetComponentEntity component : recipe) {
            recipeQty.put(component.getChildProduct().getProductId(), component.getQty());
        }

        List<PretelCount.DraftLine> draftLines = new ArrayList<>();
        List<PretelCount.Line> countLines = new ArrayList<>();
        for (PretelDraftLineDTO line : lines) {
            if (line == null || line.getProductId() == null || line.getQty() == null || line.getRecipeQty() == null) {
                return fail("Komponen pretel tidak ada di resep");
            }
            draftLines.add(new PretelCount.DraftLine(line.getProductId(), line.getQty(), line.getRecipeQty()));
            countLines.add(new PretelCount.Line(line.getQty(), line.getRecipeQty()));
        }

        String validationError = PretelCount.validate(clickCount, draftLines, recipeQty);
        if (validationError != null) {
            return fail(validationError);
        }

        int pretelCount = PretelCount.resolve(clickCount, countLines);
        if (pretelCount <= 0) {
            return new ResponseInBoolean(true, "OK");
        }

        Optional<ProductEntity> parentOpt = productRepository.findAndLockByProductId(
                draft.getParentProductId(),
                clientId
        );
        if (parentOpt.isEmpty()) {
            return fail("Barang induk tidak ditemukan");
        }

        ProductEntity parent = parentOpt.get();
        long parentStock = parent.getStock() == null ? 0L : parent.getStock();
        if (!PretelCount.enoughSetStock(parentStock, pretelCount)) {
            return fail("Stok " + parent.getFullName() + " tidak cukup untuk dipretel");
        }

        List<ProductSetComponentEntity> ordered = recipe.stream()
                .sorted(Comparator.comparing(component -> component.getChildProduct().getProductId()))
                .toList();
        List<ProductEntity> lockedChildren = new ArrayList<>();
        for (ProductSetComponentEntity component : ordered) {
            Optional<ProductEntity> childOpt = productRepository.findAndLockByProductId(
                    component.getChildProduct().getProductId(),
                    clientId
            );
            if (childOpt.isEmpty()) {
                return fail("Barang komponen tidak ditemukan");
            }
            lockedChildren.add(childOpt.get());
        }

        TransactionPretelEntity pretel = new TransactionPretelEntity();
        pretel.setTransactionEntity(transaction);
        pretel.setClientEntity(clientData);
        pretel.setParentProduct(parent);
        pretel.setSetQty((long) pretelCount);
        transactionPretelRepository.save(pretel);

        for (int index = 0; index < ordered.size(); index++) {
            ProductSetComponentEntity component = ordered.get(index);
            ProductEntity child = lockedChildren.get(index);
            long qtyIn = component.getQty() * pretelCount;
            long nextStock = (child.getStock() == null ? 0L : child.getStock()) + qtyIn;
            child.setStock(nextStock);
            productRepository.save(child);
            stockMovementService.insertKartuStok(new AdjustStockDTO(
                    child,
                    notaNumber,
                    TipeKartuStok.PRETEL,
                    qtyIn,
                    0L,
                    nextStock,
                    clientData,
                    getCurrentTimestamp()
            ));

            TransactionPretelComponentEntity snapshot = new TransactionPretelComponentEntity();
            snapshot.setTransactionPretel(pretel);
            snapshot.setChildProduct(child);
            snapshot.setQtyIn(qtyIn);
            transactionPretelComponentRepository.save(snapshot);
        }

        long nextParentStock = parentStock - pretelCount;
        parent.setStock(nextParentStock);
        productRepository.save(parent);
        stockMovementService.insertKartuStok(new AdjustStockDTO(
                parent,
                notaNumber,
                TipeKartuStok.PRETEL,
                0L,
                (long) pretelCount,
                nextParentStock,
                clientData,
                getCurrentTimestamp()
        ));
        return new ResponseInBoolean(true, "OK");
    }

    private ResponseInBoolean fail(String message) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return new ResponseInBoolean(false, message);
    }

    private void retireComponents(ProductSetEntity entity) {
        if (entity.getComponents() == null) {
            return;
        }

        for (ProductSetComponentEntity component : entity.getComponents()) {
            if (component.getDeletedAt() == null) {
                component.setDeletedAt(getCurrentTimestamp());
                productSetComponentRepository.save(component);
            }
        }
    }

    private Optional<ProductEntity> activeProduct(Long productId, Long clientId) {
        Optional<ProductEntity> product = productRepository.findFirstByProductIdAndDeletedAtIsNull(productId);
        if (product.isEmpty() || product.get().getClientEntity() == null) {
            return Optional.empty();
        }
        if (!clientId.equals(product.get().getClientEntity().getClientId())) {
            return Optional.empty();
        }
        return product;
    }

    private List<ProductSetComponentEntity> activeComponents(ProductSetEntity entity) {
        if (entity.getComponents() == null) {
            return List.of();
        }

        return entity.getComponents().stream()
                .filter(component -> component.getDeletedAt() == null)
                .toList();
    }

    private ProductSetDTO toDto(ProductSetEntity entity) {
        ProductEntity parent = entity.getParentProduct();
        List<ProductSetComponentDTO> components = activeComponents(entity).stream()
                .map(this::toComponentDto)
                .toList();
        return new ProductSetDTO(
                entity.getProductSetId(),
                parent.getProductId(),
                parent.getShortName(),
                parent.getFullName(),
                parent.getStock(),
                components
        );
    }

    private ProductSetComponentDTO toComponentDto(ProductSetComponentEntity component) {
        ProductEntity child = component.getChildProduct();
        List<ProductSetPriceDTO> prices = new ArrayList<>();
        if (child.getProductPricesEntity() != null) {
            for (ProductPricesEntity price : child.getProductPricesEntity()) {
                prices.add(new ProductSetPriceDTO(price.getPrice(), price.getMaximalCount()));
            }
        }

        return new ProductSetComponentDTO(
                child.getProductId(),
                child.getShortName(),
                child.getFullName(),
                component.getQty(),
                child.getStock(),
                child.getSupplierPrice(),
                prices
        );
    }
}
