package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnderstockPageDTO {
    private List<ProductDTO> content;
    private long totalElements;
    private int page;
    private int size;
    private List<SupplierLookupDTO> suppliers;
    private KasirClientDTO client;
    private String role;
    private String accountName;
}
