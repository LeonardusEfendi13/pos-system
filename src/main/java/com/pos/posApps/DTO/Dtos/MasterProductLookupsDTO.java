package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterProductLookupsDTO {
    private String role;
    private String accountName;
    private List<SupplierLookupDTO> suppliers;
    private List<VehicleLookupDTO> vehicles;
    private List<CategoryDTO> categories;
}
