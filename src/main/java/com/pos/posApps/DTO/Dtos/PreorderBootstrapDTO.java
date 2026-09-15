package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreorderBootstrapDTO {
    private List<SupplierLookupDTO> suppliers;
    private KasirClientDTO client;
    private PreorderDTO preorder;
    private String role;
}
