package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LunaskanPembelianDTO {
    private Long pembelianId;
    private String jenisPembayaran;
    private String rekeningAsal;
    private String rekeningTujuan;
    private MultipartFile buktiPembayaran;
}
