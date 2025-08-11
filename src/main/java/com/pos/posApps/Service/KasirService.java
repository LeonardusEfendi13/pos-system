package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.CreateProductRequest;
import com.pos.posApps.DTO.Dtos.EditProductRequest;
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

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class KasirService {
    @Autowired
    ProductRepository productRepository;

    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    ProductPricesRepository productPricesRepository;

    public List<ProductEntity> getProductData(String clientId){
        return productRepository.findAllByClientEntity_ClientId(clientId);
    }

    @Transactional
    public boolean insertProducts(CreateProductRequest req, ClientEntity clientData){
        try{
            ProductEntity productEntity = productRepository.findFirstByFullNameOrShortNameOrProductId(req.getFullName(), req.getShortName(), req.getProductId());
            if(productEntity != null){
                System.out.println("Product already exists");
                return false;
            }

            String lastProductId = productRepository.findFirstByOrderByProductIdDesc().getProductId();
            String newProductId = Generator.generateId(lastProductId == null ? "PDT0" : lastProductId);
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

            //Insert Product Prices
            String lastProductPricesId = productPricesRepository.findFirstByOrderByProductPricesIdDesc().getProductPricesId();
            String newProductPricesId = Generator.generateId(lastProductPricesId == null ? "PRS0" : lastProductPricesId);

            for(ProductPricesDTO productPricesData : req.getProductPricesDTO()){
                ProductPricesEntity newProductPrices = new ProductPricesEntity();
                newProductPrices.setProductPricesId(newProductPricesId);
                newProductPrices.setProductEntity(newProduct);
                newProductPrices.setPrice(productPricesData.getPrice());
                newProductPrices.setMinimalCount(productPricesData.getMinimalCount());
                newProductPricesId = Generator.generateId(newProductPricesId);
                productPricesRepository.save(newProductPrices);
            }
            return true;
        }catch (Exception e){
            return false;
        }

    }

    @Transactional
    public boolean editProducts(EditProductRequest req){
        ProductEntity productEntity = productRepository.findFirstByProductId(req.getProductId());
        if(productEntity == null || productEntity.getDeletedAt() != null){
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

        for(ProductPricesDTO productPrices : req.getProductPricesDTO()){
            ProductPricesEntity productPricesEntity = productPricesRepository.findFirstByProductPricesId(productPrices.getProductId());
            productPricesEntity.setProductEntity(productEntity);
            productPricesEntity.setPrice(productPrices.getPrice());
            productPricesEntity.setMinimalCount(productPrices.getMinimalCount());
            productPricesRepository.save(productPricesEntity);
        }
        return true;
    }

    @Transactional
    public boolean deleteProducts(String productId){
        ProductEntity productEntity = productRepository.findFirstByProductId(productId);
        if(productEntity == null){
            System.out.println("Product not found");
            return false;
        }
        productEntity.setDeletedAt(getCurrentTimestamp());
        productRepository.save(productEntity);

        List<ProductPricesEntity> productPricesEntity = productPricesRepository.findAllByProductEntity_ProductId(productId);

        for(ProductPricesEntity data : productPricesEntity){
            data.setDeletedAt(getCurrentTimestamp());
            productPricesRepository.save(data);
        }

        return true;
    }
}
