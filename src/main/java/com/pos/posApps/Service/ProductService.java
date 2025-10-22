package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.*;
import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import com.pos.posApps.Entity.*;
import com.pos.posApps.Repository.ProductPricesRepository;
import com.pos.posApps.Repository.ProductRepository;
import com.pos.posApps.Repository.StockMovementsRepository;
import com.pos.posApps.Repository.SupplierRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class ProductService {
    @Autowired
    ProductRepository productRepository;

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    ProductPricesRepository productPricesRepository;

    @Autowired
    StockMovementsRepository stockMovementsRepository;

    @Autowired
    StockMovementService stockMovementService;

    private ProductDTO convertToDTO(ProductEntity product) {
        return new ProductDTO(
                product.getProductId(),
                product.getShortName(),
                product.getFullName(),
                product.getSupplierPrice(),
                product.getStock(),
                product.getProductPricesEntity() == null
                        ? new ArrayList<>()
                        : product.getProductPricesEntity().stream()
                        .map(productPrices -> new ProductPricesDTO(
                                productPrices.getProductPricesId(),
                                product.getProductId(),
                                productPrices.getPercentage(),
                                productPrices.getPrice(),
                                productPrices.getMaximalCount()
                        ))
                        .collect(Collectors.toList()),
                product.getSupplierEntity().getSupplierId(),
                product.getMinimumStock()
        );
    }

    public Page<StockMovementsDTO> getStockMovementData(Long clientId, Long productId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<StockMovementsEntity> stockMovementData = stockMovementsRepository
                .findAllByClientEntity_ClientIdAndProductEntity_ProductIdAndCreatedAtBetweenAndDeletedAtIsNullOrderByStockMovementsIdAsc(
                        clientId, productId, startDate, endDate, pageable
                );

        return stockMovementData.map(data -> new StockMovementsDTO(
                data.getStockMovementsId(),
                data.getReferenceNo(),
                data.getTipeKartuStok(),
                data.getQtyIn(),
                data.getQtyOut(),
                data.getSaldo(),
                data.getCreatedAt()
        ));
    }


    public Long getStockAwalProduct(Long productId, LocalDateTime startDate) {
        List<StockMovementsEntity> stockData = stockMovementsRepository.findByProductEntity_ProductIdAndDeletedAtIsNullAndCreatedAtBefore(productId, startDate);
        return stockData.stream()
                .max(Comparator.comparing(StockMovementsEntity::getCreatedAt))
                .map(StockMovementsEntity::getSaldo)
                .orElse(0L);
    }

    public Page<ProductDTO> getProductData(Long clientId, Pageable pageable) {
        Page<ProductEntity> productData = productRepository.findAllWithPricesByClientId(clientId, pageable);
        return productData.map(this::convertToDTO);
    }

    public Page<ProductDTO> searchProductData(Long clientId, String search, Pageable pageable) {
        String trimmedSearch = (search != null) ? search.trim() : "";

        if (trimmedSearch.isEmpty()) {
            return getProductData(clientId, pageable);
        }

        Page<ProductEntity> productData = productRepository
                .searchProducts(
                        clientId,
                        trimmedSearch,
                        pageable
                );

        return productData.map(this::convertToDTO);
    }

    @Transactional
    public ResponseInBoolean insertProducts(CreateProductRequest req, ClientEntity clientData) {
        try {
            ProductEntity productEntity = productRepository.findFirstByFullNameOrShortNameOrProductIdAndClientEntity_ClientIdAndDeletedAtIsNull(req.getFullName(), req.getShortName(), req.getProductId(), clientData.getClientId());
            if (productEntity != null) {
                return new ResponseInBoolean(false, "Barang sudah ada");
            }

            Long lastProductId = productRepository.findFirstByOrderByProductIdDesc().map(ProductEntity::getProductId).orElse(0L);
            Long newProductId = Generator.generateId(lastProductId);

            Optional<SupplierEntity> supplierEntityOpt = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientData.getClientId());
            if (supplierEntityOpt.isEmpty()) {
                return new ResponseInBoolean(false, "Data Supplier tidak ditemukan");
            }

            SupplierEntity supplierEntity = supplierEntityOpt.get();

            //Insert Product
            ProductEntity newProduct = new ProductEntity();
            newProduct.setProductId(newProductId);
            newProduct.setShortName(req.getShortName());
            newProduct.setFullName(req.getFullName());
            newProduct.setSupplierPrice(req.getSupplierPrice());
            newProduct.setSupplierEntity(supplierEntity);
            newProduct.setStock(req.getStock());
            newProduct.setMinimumStock(req.getMinimumStock());
            newProduct.setClientEntity(clientData);
            productRepository.save(newProduct);

            //Insert Product Prices
            Long lastProductPricesId = productPricesRepository.findFirstByOrderByProductPricesIdDesc().map(ProductPricesEntity::getProductPricesId).orElse(0L);
            Long newProductPricesId = Generator.generateId(lastProductPricesId);

            boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                    newProduct,
                    "-",
                    TipeKartuStok.PENYESUAIAN,
                    0L,
                    0L,
                    req.getStock(),
                    clientData
            ));
            if (!isAdjusted) {
                return new ResponseInBoolean(false, "Gagal insert kartu stok");
            }

            for (ProductPricesDTO productPricesData : req.getProductPricesDTO()) {
                ProductPricesEntity newProductPrices = new ProductPricesEntity();
                newProductPrices.setProductPricesId(newProductPricesId);
                newProductPrices.setProductEntity(newProduct);
                newProductPrices.setPrice(productPricesData.getPrice());
                newProductPrices.setMaximalCount(productPricesData.getMaximalCount());
                newProductPrices.setPercentage(productPricesData.getPercentage());
                newProductPricesId = Generator.generateId(newProductPricesId);
                productPricesRepository.save(newProductPrices);
            }

            return new ResponseInBoolean(true, "Berhasil tambah produk baru");
        } catch (Exception e) {
            return new ResponseInBoolean(false, "Gagal insert produk, hubungi admin!");
        }

    }

    @Transactional
    public boolean editProducts(EditProductRequest req, ClientEntity clientEntity) {
        System.out.println("Edit req : " + req);
        Optional<ProductEntity> productEntityOpt = productRepository.findFirstByProductIdAndDeletedAtIsNull(req.getProductId());
        if (productEntityOpt.isEmpty()) {
            return false;
        }

        ProductEntity productEntity = productEntityOpt.get();

        boolean isDuplicate =
                productRepository.existsByFullNameAndClientEntity_ClientIdAndDeletedAtIsNullAndProductIdNot(req.getFullName(), clientEntity.getClientId(), req.getProductId())
                        || productRepository.existsByShortNameAndClientEntity_ClientIdAndDeletedAtIsNullAndProductIdNot(req.getShortName(), clientEntity.getClientId(), req.getProductId())
                        || productRepository.existsByProductIdAndClientEntity_ClientIdAndDeletedAtIsNullAndProductIdNot(req.getProductId(), clientEntity.getClientId(), req.getProductId());
        if (isDuplicate) {
            return false;
        }

        if (!Objects.equals(req.getStock(), productEntity.getStock())) {
            boolean isAdjusted = stockMovementService.insertKartuStok(new AdjustStockDTO(
                    productEntity,
                    "-",
                    TipeKartuStok.PENYESUAIAN,
                    0L,
                    0L,
                    req.getStock(),
                    clientEntity
            ));
            if (!isAdjusted) {
                return false;
            }
        }

        Optional<SupplierEntity> supplierEntityOpt = supplierRepository.findFirstBySupplierIdAndDeletedAtIsNullAndClientEntity_ClientId(req.getSupplierId(), clientEntity.getClientId());
        if (supplierEntityOpt.isEmpty()) {
            return false;
        }
        SupplierEntity supplierEntity = supplierEntityOpt.get();

        productEntity.setShortName(req.getShortName());
        productEntity.setFullName(req.getFullName());
        productEntity.setSupplierPrice(req.getSupplierPrice());
        productEntity.setSupplierEntity(supplierEntity);
        productEntity.setStock(req.getStock());
        productEntity.setMinimumStock(req.getMinimumStock());
        productRepository.save(productEntity);

        //Delete all product prices related to product id
        productPricesRepository.deleteAllByProductEntity_ProductId(req.getProductId());

        Long lastProductPricesId = productPricesRepository.findFirstByOrderByProductPricesIdDesc().map(ProductPricesEntity::getProductPricesId).orElse(0L);
        Long newProductPricesId = Generator.generateId(lastProductPricesId);

        for (ProductPricesDTO productPricesData : req.getProductPricesDTO()) {
            ProductPricesEntity newProductPrices = new ProductPricesEntity();
            newProductPrices.setProductPricesId(newProductPricesId);
            newProductPrices.setProductEntity(productEntity);
            newProductPrices.setPrice(productPricesData.getPrice());
            newProductPrices.setMaximalCount(productPricesData.getMaximalCount());
            newProductPrices.setPercentage(productPricesData.getPercentage());
            newProductPricesId = Generator.generateId(newProductPricesId);
            productPricesRepository.save(newProductPrices);
        }
        return true;
    }

    @Transactional
    public boolean deleteProducts(Long productId) {
        Optional<ProductEntity> productEntityOpt = productRepository.findFirstByProductIdAndDeletedAtIsNull(productId);
        if (productEntityOpt.isEmpty()) {
            return false;
        }
        ProductEntity productEntity = productEntityOpt.get();
        productEntity.setDeletedAt(getCurrentTimestamp());
        productRepository.save(productEntity);

        List<ProductPricesEntity> productPricesEntity = productPricesRepository.findAllByProductEntity_ProductIdOrderByProductPricesIdDesc(productId);

        for (ProductPricesEntity data : productPricesEntity) {
            data.setDeletedAt(getCurrentTimestamp());
            productPricesRepository.save(data);
        }

        return true;
    }

    public List<ProductDTO> searchProductByKeyword(Long clientId, String keyword, String field) {
        List<ProductEntity> products;

        if ("shortName".equalsIgnoreCase(field)) {
            products = productRepository.findByShortNameContaining(clientId, keyword);
        } else if ("fullName".equalsIgnoreCase(field)) {
            products = productRepository.findByFullNameContaining(clientId, keyword);
        } else {
            products = productRepository.findAllWithPricesByClientId(clientId, keyword);
        }

        return products.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public ProductDTO findProductByCode(Long clientId, String keyword){
        ProductEntity productData = productRepository.findByShortNameAndClientEntity_ClientIdAndDeletedAtIsNull(keyword, clientId);
        return convertToDTO(productData);
    }

    public List<ProductDTO> getUnderstockProductData(Long clientId){
        List<ProductEntity> productData = productRepository.getUnderstockProductData(clientId);
        return productData.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
}
