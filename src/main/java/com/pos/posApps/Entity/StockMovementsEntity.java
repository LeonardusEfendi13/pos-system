package com.pos.posApps.Entity;

import com.pos.posApps.DTO.Enum.EnumRole.TipeKartuStok;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "stock_movements")
public class StockMovementsEntity {

    @Id
    @Column(name = "stock_movements_id")
    private Long stockMovementsId;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "tipe")
    private TipeKartuStok tipeKartuStok;

    @Column(name = "qty_in")
    private Long qtyIn;

    @Column(name = "qty_out")
    private Long qtyOut;

    @Column(name = "saldo")
    private Long saldo;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

//    @Column(name = "tanggal_faktur")
//    private LocalDateTime tanggalFaktur;
}

