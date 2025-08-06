package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.CreateProductRequest;
import com.pos.posApps.Entity.ProductEntity;
import com.pos.posApps.Repository.ProductRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    @Autowired
    ProductRepository productRepository;

    public List<ProductEntity> getProductData(String clientId){
        return productRepository.findAllByClientId(clientId);
    }

    public boolean insertProducts(CreateProductRequest req){
        ProductEntity productEntity = productRepository.findFirstByFullNameOrShortNameOrProductId(req.getFullName(), req.getShortName(), req.getProductId());
        if(productEntity != null){
            System.out.println("Product already exists");
            return false;
        }

        String lastProductId = productRepository.findFirstByOrderByProductIdDesc().getProductId();
        String newProductId = Generator.generateId(lastProductId == null ? "PDT0" : lastProductId);

        ProductEntity newProduct = new ProductEntity();
        newProduct.setProductId(newProductId);
        //todo

        return true;
    }
}
