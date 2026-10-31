package com.pos.posApps.Repository;

import com.pos.posApps.Entity.StagingPriceListEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Batched JDBC writes for the price list import. JPA saveAll on these tables
 * runs one statement per row (assigned id -> merge, identity id -> no batching),
 * which takes minutes for a 60k-row price list.
 */
@Repository
public class PriceListBulkWriter {
    public static final int CHUNK_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;

    public PriceListBulkWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record PriceRow(String partNumber, BigDecimal harga) {
    }

    public record CodeOwner(String partNumber, String merk) {
    }

    public List<CodeOwner> findCodeOwners() {
        return jdbcTemplate.query(
                "select part_number, merk from price_list",
                (rs, rowNum) -> new CodeOwner(rs.getString("part_number"), rs.getString("merk"))
        );
    }

    /** Inserts in chunks and reports the running count after each chunk. */
    public void insertPrices(String merk, List<PriceRow> rows, IntConsumer onChunk) {
        String sql = "insert into price_list (part_number, harga_jual, merk) values (?, ?, ?)";
        int written = 0;
        for (int from = 0; from < rows.size(); from += CHUNK_SIZE) {
            List<PriceRow> chunk = rows.subList(from, Math.min(from + CHUNK_SIZE, rows.size()));
            List<Object[]> args = new ArrayList<>(chunk.size());
            for (PriceRow row : chunk) {
                args.add(new Object[]{row.partNumber(), row.harga(), merk});
            }
            jdbcTemplate.batchUpdate(sql, args);
            written += chunk.size();
            onChunk.accept(written);
        }
    }

    public void insertStaging(List<StagingPriceListEntity> rows) {
        String sql = "insert into staging_price_list "
                + "(merk, part_number, nama, harga, harga_text, excel_row, status, error_message) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?)";
        for (int from = 0; from < rows.size(); from += CHUNK_SIZE) {
            List<StagingPriceListEntity> chunk = rows.subList(from, Math.min(from + CHUNK_SIZE, rows.size()));
            List<Object[]> args = new ArrayList<>(chunk.size());
            for (StagingPriceListEntity row : chunk) {
                args.add(new Object[]{
                        row.getMerk(),
                        row.getPartNumber(),
                        row.getNama(),
                        row.getHarga(),
                        row.getHargaText(),
                        row.getExcelRow(),
                        row.getStatus(),
                        row.getErrorMessage()
                });
            }
            jdbcTemplate.batchUpdate(sql, args);
        }
    }
}
