package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "price_list")
public class PriceListEntity {

    @Id
    @Column(name = "part_number")
    private String partNumber;

    @Column(name = "harga_jual")
    private BigDecimal hargaJual;

    @Column(name = "merk")
    private String merk;
}

