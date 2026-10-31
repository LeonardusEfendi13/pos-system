package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.PriceListImportJobDTO;
import com.pos.posApps.DTO.Dtos.PriceListImportPreviewDTO;
import com.pos.posApps.Entity.PriceListEntity;
import com.pos.posApps.Entity.StagingPriceListEntity;
import com.pos.posApps.Repository.PriceListBulkWriter;
import com.pos.posApps.Repository.PriceListRepository;
import com.pos.posApps.Repository.StagingPriceListRepository;
import com.pos.posApps.Util.PriceListExcelReader;
import com.pos.posApps.Util.PriceListHargaParser;
import com.pos.posApps.Util.PriceListStagingClassifier;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class PriceListImportService {
    static final long MAX_FILE_BYTES = 20L * 1024L * 1024L;
    private static final String HONDA = "HONDA";
    private static final String YAMAHA = "YAMAHA";

    private static final String RUNNING_MESSAGE = "Impor harga sedang berjalan. Tunggu sampai selesai.";
    private static final Logger log = LoggerFactory.getLogger(PriceListImportService.class);

    private final StagingPriceListRepository stagingPriceListRepository;
    private final PriceListRepository priceListRepository;
    private final PriceListBulkWriter bulkWriter;
    private final PriceListImportJobRegistry jobRegistry;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "price-list-import");
        thread.setDaemon(true);
        return thread;
    });

    public PriceListImportService(
            StagingPriceListRepository stagingPriceListRepository,
            PriceListRepository priceListRepository,
            PriceListBulkWriter bulkWriter,
            PriceListImportJobRegistry jobRegistry,
            PlatformTransactionManager transactionManager
    ) {
        this.stagingPriceListRepository = stagingPriceListRepository;
        this.priceListRepository = priceListRepository;
        this.bulkWriter = bulkWriter;
        this.jobRegistry = jobRegistry;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public record PreviewResult(String error, PriceListImportPreviewDTO preview) {
    }

    public enum StartStatus { ACCEPTED, REJECTED, CONFLICT }

    public record StartResult(StartStatus status, String message, PriceListImportJobDTO job) {
        static StartResult accepted(PriceListImportJobDTO job) {
            return new StartResult(StartStatus.ACCEPTED, "", job);
        }

        static StartResult rejected(String message) {
            return new StartResult(StartStatus.REJECTED, message, null);
        }

        static StartResult conflict(String message, PriceListImportJobDTO job) {
            return new StartResult(StartStatus.CONFLICT, message, job);
        }
    }

    /** Rejection inside the job; rolls the transaction back and keeps the old prices. */
    private static final class ConfirmRejected extends RuntimeException {
        private ConfirmRejected(String message) {
            super(message);
        }
    }

    @Transactional
    public PreviewResult preview(String merkRaw, MultipartFile file) {
        String merk = normalizeMerk(merkRaw);
        if (merk == null) {
            return new PreviewResult("Merk harus Honda atau Yamaha.", null);
        }
        if (jobRegistry.isRunning()) {
            return new PreviewResult(RUNNING_MESSAGE, null);
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
        bulkWriter.insertStaging(entities);
        return new PreviewResult(null, toPreview(merk, staged, existing));
    }

    /**
     * Cheap checks run synchronously; the heavy replace runs as a background job
     * so the HTTP call returns at once and can never hit a client timeout.
     */
    public StartResult startConfirm(String merkRaw) {
        String merk = normalizeMerk(merkRaw);
        if (merk == null) {
            return StartResult.rejected("Merk harus Honda atau Yamaha.");
        }
        if (jobRegistry.isRunning()) {
            return StartResult.conflict(RUNNING_MESSAGE, jobRegistry.active().orElse(null));
        }
        if (stagingPriceListRepository.countByMerk(merk) == 0) {
            return StartResult.rejected("Tidak ada data staging. Unggah file terlebih dahulu.");
        }
        long blocking = stagingPriceListRepository.countByMerkAndStatusNotIn(
                merk,
                List.of(PriceListStagingClassifier.READY, PriceListStagingClassifier.DUPLICATE)
        );
        if (blocking > 0) {
            return StartResult.rejected("Masih ada baris yang menghalangi. Perbaiki file, lalu unggah ulang.");
        }

        PriceListImportJobRegistry.StartResult start = jobRegistry.start(merk);
        if (!start.started()) {
            return StartResult.conflict(RUNNING_MESSAGE, start.job());
        }
        String jobId = start.job().jobId();
        executor.submit(() -> runConfirm(jobId, merk));
        return StartResult.accepted(start.job());
    }

    public Optional<PriceListImportJobDTO> job(String jobId) {
        return jobRegistry.get(jobId);
    }

    public Optional<PriceListImportJobDTO> activeJob() {
        return jobRegistry.active();
    }

    void runConfirm(String jobId, String merk) {
        try {
            String message = transactionTemplate.execute(status -> replacePrices(jobId, merk));
            jobRegistry.finish(jobId, message);
        } catch (ConfirmRejected rejected) {
            jobRegistry.fail(jobId, rejected.getMessage() + " Harga lama tidak berubah.");
        } catch (RuntimeException exception) {
            log.error("Price list confirm failed for {}", merk, exception);
            jobRegistry.fail(jobId, "Gagal menulis harga ke database. Harga lama tidak berubah.");
        }
    }

    private String replacePrices(String jobId, String merk) {
        jobRegistry.progress(jobId, PriceListImportJobRegistry.PHASE_CHECKING, 0, 0);
        List<StagingPriceListEntity> staged = stagingPriceListRepository.findAllByMerkOrderByExcelRowAsc(merk);
        if (staged.isEmpty()) {
            throw new ConfirmRejected("Tidak ada data staging. Unggah file terlebih dahulu.");
        }

        List<PriceListBulkWriter.PriceRow> ready = new ArrayList<>();
        Set<String> readyCodes = new HashSet<>();
        for (StagingPriceListEntity row : staged) {
            if (PriceListStagingClassifier.READY.equals(row.getStatus())) {
                String code = PriceListHargaParser.normalizeCode(row.getPartNumber());
                if (code.isEmpty() || row.getHarga() == null || !readyCodes.add(code)) {
                    throw new ConfirmRejected("Staging tidak aman. Unggah ulang file.");
                }
                ready.add(new PriceListBulkWriter.PriceRow(code, row.getHarga()));
            } else if (!PriceListStagingClassifier.DUPLICATE.equals(row.getStatus())) {
                throw new ConfirmRejected("Masih ada baris yang menghalangi. Perbaiki file, lalu unggah ulang.");
            }
        }
        if (ready.isEmpty()) {
            throw new ConfirmRejected("Tidak ada baris yang bisa diimpor.");
        }

        int total = ready.size();
        int absent = 0;
        for (PriceListBulkWriter.CodeOwner existing : bulkWriter.findCodeOwners()) {
            String code = PriceListHargaParser.normalizeCode(existing.partNumber());
            String owner = storedMerk(existing.merk());
            if (owner.isEmpty()) {
                owner = "MERK LAIN";
            }
            if (readyCodes.contains(code) && !owner.equals(merk)) {
                throw new ConfirmRejected("Kode " + code + " sudah terdaftar di " + owner + ".");
            }
            if (owner.equals(merk) && !readyCodes.contains(code)) {
                absent++;
            }
        }

        jobRegistry.progress(jobId, PriceListImportJobRegistry.PHASE_DELETING, 0, total);
        priceListRepository.deleteByNormalizedMerk(merk);

        jobRegistry.progress(jobId, PriceListImportJobRegistry.PHASE_WRITING, 0, total);
        bulkWriter.insertPrices(
                merk,
                ready,
                written -> jobRegistry.progress(jobId, PriceListImportJobRegistry.PHASE_WRITING, written, total)
        );
        stagingPriceListRepository.deleteByMerk(merk);
        return "Harga " + merk + " diperbarui. " + total + " kode ditulis, " + absent + " kode yang tidak ada di file dihapus.";
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
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
