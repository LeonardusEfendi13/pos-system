package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.PriceListImportJobDTO;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory state of price list confirm jobs. Only one job may run at a time
 * across all brands, because Honda and Yamaha share the price_list table.
 * Finished jobs are kept for a while so a refreshed page can still show the result.
 */
@Component
public class PriceListImportJobRegistry {
    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    public static final String PHASE_CHECKING = "CHECKING";
    public static final String PHASE_DELETING = "DELETING";
    public static final String PHASE_WRITING = "WRITING";
    public static final String PHASE_DONE = "DONE";

    static final Duration RETENTION = Duration.ofMinutes(10);

    private final Clock clock;
    private final Map<String, Job> jobs = new LinkedHashMap<>();

    public PriceListImportJobRegistry() {
        this(Clock.systemUTC());
    }

    PriceListImportJobRegistry(Clock clock) {
        this.clock = clock;
    }

    public record StartResult(PriceListImportJobDTO job, boolean started) {
    }

    private static final class Job {
        private final String jobId;
        private final String merk;
        private final Instant startedAt;
        private String status = RUNNING;
        private String phase = PHASE_CHECKING;
        private int processed;
        private int total;
        private String message = "";
        private Instant finishedAt;

        private Job(String jobId, String merk, Instant startedAt) {
            this.jobId = jobId;
            this.merk = merk;
            this.startedAt = startedAt;
        }
    }

    public synchronized StartResult start(String merk) {
        prune();
        Optional<Job> running = findRunning();
        if (running.isPresent()) {
            return new StartResult(toDto(running.get()), false);
        }
        Job job = new Job(UUID.randomUUID().toString(), merk, clock.instant());
        jobs.put(job.jobId, job);
        return new StartResult(toDto(job), true);
    }

    public synchronized boolean isRunning() {
        return findRunning().isPresent();
    }

    public synchronized void progress(String jobId, String phase, int processed, int total) {
        Job job = jobs.get(jobId);
        if (job == null || !RUNNING.equals(job.status)) {
            return;
        }
        job.phase = phase;
        job.processed = processed;
        job.total = total;
    }

    public synchronized void finish(String jobId, String message) {
        Job job = jobs.get(jobId);
        if (job == null) {
            return;
        }
        job.status = DONE;
        job.phase = PHASE_DONE;
        job.processed = job.total;
        job.message = message;
        job.finishedAt = clock.instant();
    }

    public synchronized void fail(String jobId, String message) {
        Job job = jobs.get(jobId);
        if (job == null) {
            return;
        }
        job.status = FAILED;
        job.message = message;
        job.finishedAt = clock.instant();
    }

    public synchronized Optional<PriceListImportJobDTO> get(String jobId) {
        prune();
        return Optional.ofNullable(jobs.get(jobId)).map(this::toDto);
    }

    /** The running job, else the most recently finished one still within retention. */
    public synchronized Optional<PriceListImportJobDTO> active() {
        prune();
        Optional<Job> running = findRunning();
        if (running.isPresent()) {
            return running.map(this::toDto);
        }
        return jobs.values().stream()
                .max(Comparator.comparing(job -> job.finishedAt))
                .map(this::toDto);
    }

    private Optional<Job> findRunning() {
        return jobs.values().stream().filter(job -> RUNNING.equals(job.status)).findFirst();
    }

    private void prune() {
        Instant cutoff = clock.instant().minus(RETENTION);
        jobs.values().removeIf(job -> job.finishedAt != null && job.finishedAt.isBefore(cutoff));
    }

    private PriceListImportJobDTO toDto(Job job) {
        Instant end = job.finishedAt == null ? clock.instant() : job.finishedAt;
        return new PriceListImportJobDTO(
                job.jobId,
                job.merk,
                job.status,
                job.phase,
                job.processed,
                job.total,
                job.message,
                Math.max(0, Duration.between(job.startedAt, end).getSeconds())
        );
    }
}
