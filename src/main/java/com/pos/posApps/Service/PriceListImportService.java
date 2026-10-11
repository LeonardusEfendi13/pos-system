package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.PriceListImportPreviewDTO;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.PriceListEntity;
import com.pos.posApps.Entity.StagingPriceListEntity;
import com.pos.posApps.Repository.PriceListRepository;
import com.pos.posApps.Repository.StagingPriceListRepository;
import com.pos.posApps.Util.PriceListExcelReader;
import com.pos.posApps.Util.PriceListHargaParser;
import com.pos.posApps.Util.PriceListStagingClassifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PriceListImportService {
    static final long MAX_FILE_BYTES = 20L * 1024L * 1024L;
    private static final String HONDA = "HONDA";
    private static final String YAMAHA = "YAMAHA";

    private final StagingPriceListRepository stagingPriceListRepository;
    private final PriceListRepository priceListRepository;

    public PriceListImportService(
            StagingPriceListRepository stagingPriceListRepository,
            PriceListRepository priceListRepository
    ) {
        this.stagingPriceListRepository = stagingPriceListRepository;
        this.priceListRepository = priceListRepository;
    }

    public record PreviewResult(String error, PriceListImportPreviewDTO preview) {
    }

    @Transactional
    public PreviewResult preview(String merkRaw, MultipartFile file) {
        String merk = normalizeMerk(merkRaw);
        if (merk == null) {
            return new PreviewResult("Merk harus Honda atau Yamaha.", null);
        }
        if (file == null || file.isEmpty()) {
            return new PreviewResult("File Excel kosong.", null);
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            return new PreviewResult("File terlalu besar. Maksimal 20 MB.", null);
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            return new PreviewResult("File harus berformat .xlsx.", null);
        }

        PriceListExcelReader.PriceListSheet sheet;
        try {
            sheet = PriceListExcelReader.read(file.getInputStream());
        } catch (Exception exception) {
            return new PreviewResult("File Excel tidak bisa dibaca.", null);
        }
        if (sheet.error() != null) {
            return new PreviewResult(sheet.error(), null);
        }

        Map<String, String> partMerk = new HashMap<>();
        List<PriceListEntity> existing = priceListRepository.findAll();
        for (PriceListEntity row : existing) {
            String code = PriceListHargaParser.normalizeCode(row.getPartNumber());
            if (code.isEmpty()) {
                continue;
            }
            String owner = storedMerk(row.getMerk());
            partMerk.put(code, owner.isEmpty() ? "MERK LAIN" : owner);
        }

        List<PriceListStagingClassifier.StagedPriceRow> staged = PriceListStagingClassifier.classify(
                sheet.rows(),
                merk,
                partMerk
        );
        stagingPriceListRepository.deleteByMerk(merk);
        List<StagingPriceListEntity> entities = new ArrayList<>();
        for (PriceListStagingClassifier.StagedPriceRow row : staged) {
            StagingPriceListEntity entity = new StagingPriceListEntity();
            entity.setMerk(merk);
            entity.setPartNumber(row.partNumber());
            entity.setNama(row.nama());
            entity.setHarga(row.harga());
            entity.setHargaText(row.hargaText());
            entity.setExcelRow(row.excelRow());
            entity.setStatus(row.status());
            entity.setErrorMessage(row.errorMessage());
            entities.add(entity);
        }
        stagingPriceListRepository.saveAll(entities);
        return new PreviewResult(null, toPreview(merk, staged, existing));
    }

    @Transactional
    public ResponseInBoolean confirm(String merkRaw) {
        String merk = normalizeMerk(merkRaw);
        if (merk == null) {
            return new ResponseInBoolean(false, "Merk harus Honda atau Yamaha.");
        }

        List<StagingPriceListEntity> staged = stagingPriceListRepository.findAllByMerkOrderByExcelRowAsc(merk);
        if (staged.isEmpty()) {
            return new ResponseInBoolean(false, "Tidak ada data staging. Unggah file terlebih dahulu.");
        }

        List<StagingPriceListEntity> ready = new ArrayList<>();
        Set<String> readyCodes = new HashSet<>();
        boolean blocking = false;
        for (StagingPriceListEntity row : staged) {
            if (PriceListStagingClassifier.READY.equals(row.getStatus())) {
                String code = PriceListHargaParser.normalizeCode(row.getPartNumber());
                if (code.isEmpty() || row.getHarga() == null || !readyCodes.add(code)) {
                    return new ResponseInBoolean(false, "Staging tidak aman. Unggah ulang file.");
                }
                ready.add(row);
            } else if (!PriceListStagingClassifier.DUPLICATE.equals(row.getStatus())) {
                blocking = true;
            }
        }
        if (blocking) {
            return new ResponseInBoolean(false, "Masih ada baris yang menghalangi. Perbaiki file, lalu unggah ulang.");
        }
        if (ready.isEmpty()) {
            return new ResponseInBoolean(false, "Tidak ada baris yang bisa diimpor.");
        }

        int absent = 0;
        for (PriceListEntity existing : priceListRepository.findAll()) {
            String code = PriceListHargaParser.normalizeCode(existing.getPartNumber());
            String owner = storedMerk(existing.getMerk());
            if (owner.isEmpty()) {
                owner = "MERK LAIN";
            }
            if (readyCodes.contains(code) && !owner.equals(merk)) {
                return new ResponseInBoolean(false, "Kode " + code + " sudah terdaftar di " + owner + ".");
            }
            if (owner.equals(merk) && !readyCodes.contains(code)) {
                absent++;
            }
        }

        priceListRepository.deleteByNormalizedMerk(merk);
        List<PriceListEntity> inserts = new ArrayList<>();
        for (StagingPriceListEntity row : ready) {
            PriceListEntity entity = new PriceListEntity();
            entity.setPartNumber(PriceListHargaParser.normalizeCode(row.getPartNumber()));
            entity.setHargaJual(row.getHarga());
            entity.setMerk(merk);
            inserts.add(entity);
        }
        priceListRepository.saveAll(inserts);
        stagingPriceListRepository.deleteByMerk(merk);
        return new ResponseInBoolean(
                true,
                "Harga " + merk + " diperbarui. " + inserts.size() + " kode ditulis, " + absent + " kode yang tidak ada di file dihapus."
        );
    }

    private PriceListImportPreviewDTO toPreview(
            String merk,
            List<PriceListStagingClassifier.StagedPriceRow> staged,
            List<PriceListEntity> existing
    ) {
        int readyCount = 0;
        int duplicateCount = 0;
        int blockingCount = 0;
        Set<String> readyCodes = new HashSet<>();
        List<PriceListImportPreviewDTO.BlockingRow> blockingRows = new ArrayList<>();

        for (PriceListStagingClassifier.StagedPriceRow row : staged) {
            if (PriceListStagingClassifier.READY.equals(row.status())) {
                readyCount++;
                readyCodes.add(row.partNumber());
            } else if (PriceListStagingClassifier.DUPLICATE.equals(row.status())) {
                duplicateCount++;
            } else {
                blockingCount++;
                if (blockingRows.size() < 200) {
                    blockingRows.add(new PriceListImportPreviewDTO.BlockingRow(
                            row.excelRow(),
                            row.partNumber(),
                            row.nama() == null ? "" : row.nama(),
                            row.hargaText() == null ? "" : row.hargaText(),
                            row.errorMessage()
                    ));
                }
            }
        }

        List<PriceListImportPreviewDTO.DeletingRow> deletingRows = new ArrayList<>();
        int deleteCount = 0;
        for (PriceListEntity row : existing) {
            if (!merk.equals(storedMerk(row.getMerk()))) {
                continue;
            }
            String code = PriceListHargaParser.normalizeCode(row.getPartNumber());
            if (readyCodes.contains(code)) {
                continue;
            }
            deleteCount++;
            if (deletingRows.size() >= 100) {
                continue;
            }
            deletingRows.add(new PriceListImportPreviewDTO.DeletingRow(
                    row.getPartNumber(),
                    displayHarga(row.getHargaJual())
            ));
        }

        boolean canConfirm = blockingCount == 0 && readyCount > 0;
        String message;
        if (blockingCount > 0) {
            message = "Masih ada baris yang menghalangi. Perbaiki file, lalu unggah ulang.";
        } else if (readyCount == 0) {
            message = "Tidak ada baris yang bisa diimpor.";
        } else {
            message = "Staging aman. Confirm akan mengganti seluruh harga " + merk + ".";
        }

        return new PriceListImportPreviewDTO(
                merk,
                readyCount,
                duplicateCount,
                blockingCount,
                deleteCount,
                canConfirm,
                message,
                blockingRows,
                deletingRows
        );
    }

    private static String displayHarga(BigDecimal harga) {
        return PriceListHargaParser.display(harga);
    }

    private static String storedMerk(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    static String normalizeMerk(String raw) {
        if (raw == null) {
            return null;
        }
        String merk = raw.trim().toUpperCase(Locale.ROOT);
        if (HONDA.equals(merk) || YAMAHA.equals(merk)) {
            return merk;
        }
        return null;
    }
}
