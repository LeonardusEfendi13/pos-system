package com.pos.posApps.DTO.Dtos;

public record PriceListImportJobDTO(
        String jobId,
        String merk,
        String status,
        String phase,
        int processed,
        int total,
        String message,
        long elapsedSeconds
) {
}
