package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaporanNilaiPersediaanPageDTO {
    private List<LaporanNilaiPersediaanDTO> content;
    private long totalElements;
    private int page;
    private int size;
    private String totalAsset;
    private String role;
    private String accountName;
}
