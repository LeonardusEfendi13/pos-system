package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PembelianBootstrapDTO {
    private List<SupplierLookupDTO> suppliers;
    private KasirClientDTO client;
    private PembelianDTO pembelian;
    private ConvertToPembelianDTO convert;
    private String role;
}
