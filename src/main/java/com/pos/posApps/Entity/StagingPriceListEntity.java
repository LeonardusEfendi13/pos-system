package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "staging_price_list")
public class StagingPriceListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staging_price_list_id")
    private Long stagingPriceListId;

    @Column(name = "merk")
    private String merk;

    @Column(name = "part_number")
    private String partNumber;

    @Column(name = "nama")
    private String nama;

    @Column(name = "harga")
    private BigDecimal harga;

    @Column(name = "harga_text")
    private String hargaText;

    @Column(name = "excel_row")
    private Integer excelRow;

    @Column(name = "status")
    private String status;

    @Column(name = "error_message")
    private String errorMessage;
}
