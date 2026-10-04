package com.pos.posApps.DTO.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanPelangganTransaksiDTO {
    private LocalDateTime transactionDate;
    private String transactionNumber;
    private BigDecimal totalPrice;
    @JsonProperty("isPaid")
    private boolean isPaid;
}
