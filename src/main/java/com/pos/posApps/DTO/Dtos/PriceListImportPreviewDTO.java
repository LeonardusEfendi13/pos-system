package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceListImportPreviewDTO {
    private String merk;
    private int readyCount;
    private int duplicateCount;
    private int blockingCount;
    private int deleteCount;
    private boolean canConfirm;
    private String message;
    private List<BlockingRow> blockingRows;
    private List<DeletingRow> deletingRows;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockingRow {
        private int excelRow;
        private String kode;
        private String nama;
        private String hargaText;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeletingRow {
        private String kode;
        private String harga;
    }
}
