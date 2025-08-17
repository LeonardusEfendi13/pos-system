package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.CreateProductRequest;
import com.pos.posApps.DTO.Dtos.EditProductRequest;
import com.pos.posApps.DTO.Dtos.ProductDTO;
import com.pos.posApps.DTO.Dtos.ProductPricesDTO;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.ProductEntity;
import com.pos.posApps.Entity.ProductPricesEntity;
import com.pos.posApps.Entity.SupplierEntity;
import com.pos.posApps.Repository.ProductPricesRepository;
import com.pos.posApps.Repository.ProductRepository;
import com.pos.posApps.Repository.SupplierRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public List<ProductDTO> getProductData(String clientId) {
        List<ProductEntity> productData = productRepository.findAllByClientEntity_ClientIdAndProductPricesEntityIsNotNullOrderByProductIdAsc(clientId);
        return productData.stream().map(product -> new ProductDTO(
                product.getProductId(),
                product.getShortName(),
                product.getFullName(),
                product.getSupplierPrice(),
                product.getStock(),
                product.getProductPricesEntity().stream()
                        .map(productPrices -> new ProductPricesDTO(
                                productPrices.getProductPricesId(),
                                product.getProductId(),
                                productPrices.getPercentage(),
                                productPrices.getPrice(),
                                productPrices.getMaximalCount()
                        ))
                        .collect(Collectors.toList()),  // collect the stream to a list
                product.getSupplierEntity().getSupplierId()
        )).collect(Collectors.toList());
    }

    @Transactional
    public boolean insertProducts(CreateProductRequest req, ClientEntity clientData){
        try{
            ProductEntity productEntity = productRepository.findFirstByFullNameOrShortNameOrProductId(req.getFullName(), req.getShortName(), req.getProductId());
            if(productEntity != null){
                System.out.println("Product already exists");
                return false;
            }

            String lastProductId = productRepository.findFirstByOrderByProductIdDesc().map(ProductEntity::getProductId).orElse("PDT0");
            String newProductId = Generator.generateId(lastProductId);

            SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierId(req.getSupplierId());
            if(supplierEntity == null){
                System.out.println("Can't find supplier with id : " + req.getSupplierId());
                return false;
            }

            //Insert Product
            ProductEntity newProduct = new ProductEntity();
            newProduct.setProductId(newProductId);
            newProduct.setShortName(req.getShortName());
            newProduct.setFullName(req.getFullName());
            newProduct.setSupplierPrice(req.getSupplierPrice());
            newProduct.setSupplierEntity(supplierEntity);
            newProduct.setStock(req.getStock());
            newProduct.setClientEntity(clientData);
            productRepository.save(newProduct);

            System.out.println("success save new product");
            //Insert Product Prices
            String lastProductPricesId = productPricesRepository.findFirstByOrderByProductPricesIdDesc().map(ProductPricesEntity::getProductPricesId).orElse("PRS0");
            String newProductPricesId = Generator.generateId(lastProductPricesId);

            for(ProductPricesDTO productPricesData : req.getProductPricesDTO()){
                System.out.println("Entering loop product prices ");
                if(productPricesData != null){
                        ProductPricesEntity newProductPrices = new ProductPricesEntity();
                        newProductPrices.setProductPricesId(newProductPricesId);
                        newProductPrices.setProductEntity(newProduct);
                        newProductPrices.setPrice(productPricesData.getPrice());
                        newProductPrices.setMaximalCount(productPricesData.getMaximalCount());
                        newProductPrices.setPercentage(productPricesData.getPercentage());
                        newProductPricesId = Generator.generateId(newProductPricesId);
                        productPricesRepository.save(newProductPrices);
                }
            }
            return true;
        }catch (Exception e){
            System.out.println("Exception : " + e);
            return false;
        }

    }

    @Transactional
    public boolean editProducts(EditProductRequest req){
        ProductEntity productEntity = productRepository.findFirstByProductIdAndDeletedAtIsNull(req.getProductId());
        if(productEntity == null){
            System.out.println("Product not found");
            return false;
        }

        SupplierEntity supplierEntity = supplierRepository.findFirstBySupplierId(req.getSupplierId());
        if(supplierEntity == null){
            System.out.println("Can't find supplier with id : " + req.getSupplierId());
            return false;
        }
        productEntity.setShortName(req.getShortName());
        productEntity.setFullName(req.getFullName());
        productEntity.setSupplierPrice(req.getSupplierPrice());
        productEntity.setSupplierEntity(supplierEntity);
        productEntity.setStock(req.getStock());
        productRepository.save(productEntity);

        //Delete all product prices related to product id
        productPricesRepository.deleteAllByProductEntity_ProductId(req.getProductId());

        String lastProductPricesId = productPricesRepository.findFirstByOrderByProductPricesIdDesc().map(ProductPricesEntity::getProductPricesId).orElse("PRS0");
        String newProductPricesId = Generator.generateId(lastProductPricesId);

        for(ProductPricesDTO productPricesData : req.getProductPricesDTO()){
            System.out.println("Entering loop product prices ");
            if(productPricesData != null){
                ProductPricesEntity newProductPrices = new ProductPricesEntity();
                newProductPrices.setProductPricesId(newProductPricesId);
                newProductPrices.setProductEntity(productEntity);
                newProductPrices.setPrice(productPricesData.getPrice());
                newProductPrices.setMaximalCount(productPricesData.getMaximalCount());
                newProductPrices.setPercentage(productPricesData.getPercentage());
                newProductPricesId = Generator.generateId(newProductPricesId);
                productPricesRepository.save(newProductPrices);
            }
        }

//        for(ProductPricesDTO productPrices : req.getProductPricesDTO()){
//            ProductPricesEntity productPricesEntity = productPricesRepository.findFirstByProductPricesId(productPrices.getProductPricesId());
//            productPricesEntity.setProductEntity(productEntity);
//            productPricesEntity.setPrice(productPrices.getPrice());
//            productPricesEntity.setPercentage(productPrices.getPercentage());
//            productPricesEntity.setMaximalCount(productPrices.getMaximalCount());
//            productPricesRepository.save(productPricesEntity);
//        }
        return true;
    }

    @Transactional
    public boolean deleteProducts(String productId){
        ProductEntity productEntity = productRepository.findFirstByProductIdAndDeletedAtIsNull(productId);
        if(productEntity == null){
            System.out.println("Product not found");
            return false;
        }
        productEntity.setDeletedAt(getCurrentTimestamp());
        productRepository.save(productEntity);

        List<ProductPricesEntity> productPricesEntity = productPricesRepository.findAllByProductEntity_ProductIdOrderByProductPricesIdAsc(productId);

        for(ProductPricesEntity data : productPricesEntity){
            data.setDeletedAt(getCurrentTimestamp());
            productPricesRepository.save(data);
        }

        return true;
    }
}
