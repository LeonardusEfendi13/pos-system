package com.pos.posApps.Service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class PostgresBackupService {

    public File backupDatabase(String dbName) throws IOException, InterruptedException {

        String fileName = "backup-" + dbName + "-" + System.currentTimeMillis() + ".sql";
        File backupFile = new File(System.getProperty("java.io.tmpdir"), fileName);

        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", "localhost",
                "-p", "5432",
                "-U", "postgres",
                "-F", "p",          // plain SQL
                "-b",               // include blobs
                "-v",               // verbose
                dbName
        );

        pb.environment().put("PGPASSWORD", "Anjenk132");
        pb.redirectOutput(backupFile);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Backup failed with exit code " + exitCode);
        }

        return backupFile;
    }
}
