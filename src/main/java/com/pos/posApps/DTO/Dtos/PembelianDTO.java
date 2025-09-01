package com.pos.posApps.DTO.Dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PembelianDTO {
    private Long pembelianId;
    private String noFaktur;
    private LocalDateTime tanggalBeli;
    private boolean isCash;
    private LocalDateTime tanggalTempo;
    private BigDecimal totalPrice;
    private SupplierDTO supplierDTO;
    private boolean isPaid;
    private List<PembelianDetailDTO> pembelianDetailDTOS;
}
