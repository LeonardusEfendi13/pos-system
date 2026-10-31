package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.PriceListImportJobDTO;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceListImportJobRegistryTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-31T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void onlyOneJobRunsAtATime() {
        PriceListImportJobRegistry registry = new PriceListImportJobRegistry(new MutableClock());

        PriceListImportJobRegistry.StartResult first = registry.start("HONDA");
        PriceListImportJobRegistry.StartResult second = registry.start("YAMAHA");

        assertTrue(first.started());
        assertFalse(second.started());
        assertEquals(first.job().jobId(), second.job().jobId());
        assertTrue(registry.isRunning());
    }

    @Test
    void progressAndFinishAreReported() {
        MutableClock clock = new MutableClock();
        PriceListImportJobRegistry registry = new PriceListImportJobRegistry(clock);
        String jobId = registry.start("HONDA").job().jobId();

        registry.progress(jobId, PriceListImportJobRegistry.PHASE_WRITING, 1000, 59466);
        clock.advance(Duration.ofSeconds(4));
        PriceListImportJobDTO running = registry.get(jobId).orElseThrow();

        assertEquals(PriceListImportJobRegistry.RUNNING, running.status());
        assertEquals(PriceListImportJobRegistry.PHASE_WRITING, running.phase());
        assertEquals(1000, running.processed());
        assertEquals(59466, running.total());
        assertEquals(4, running.elapsedSeconds());

        registry.finish(jobId, "Harga HONDA diperbarui.");
        PriceListImportJobDTO done = registry.get(jobId).orElseThrow();

        assertEquals(PriceListImportJobRegistry.DONE, done.status());
        assertEquals(59466, done.processed());
        assertEquals("Harga HONDA diperbarui.", done.message());
        assertFalse(registry.isRunning());
        assertTrue(registry.start("YAMAHA").started());
    }

    @Test
    void progressIsIgnoredAfterFailure() {
        PriceListImportJobRegistry registry = new PriceListImportJobRegistry(new MutableClock());
        String jobId = registry.start("HONDA").job().jobId();

        registry.fail(jobId, "Kode X sudah terdaftar di YAMAHA.");
        registry.progress(jobId, PriceListImportJobRegistry.PHASE_WRITING, 10, 20);
        PriceListImportJobDTO failed = registry.get(jobId).orElseThrow();

        assertEquals(PriceListImportJobRegistry.FAILED, failed.status());
        assertEquals(0, failed.processed());
    }

    @Test
    void finishedJobIsActiveUntilRetentionEnds() {
        MutableClock clock = new MutableClock();
        PriceListImportJobRegistry registry = new PriceListImportJobRegistry(clock);
        String jobId = registry.start("HONDA").job().jobId();
        registry.finish(jobId, "ok");

        clock.advance(Duration.ofMinutes(9));
        assertEquals(jobId, registry.active().orElseThrow().jobId());

        clock.advance(Duration.ofMinutes(2));
        assertTrue(registry.active().isEmpty());
        assertTrue(registry.get(jobId).isEmpty());
    }
}
