package com.pos.posApps.Service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PostgresBackupService {

    public File backupDatabase(String dbName)
            throws IOException, InterruptedException {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);

        // IMPORTANT: use .dump, not .sql
        String fileName = "backup-AnugrahMotor-" + timestamp + ".dump";

        File backupFile = new File(
                System.getProperty("java.io.tmpdir"),
                fileName
        );

        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", "localhost",
                "-p", "5432",
                "-U", "postgres",
                "-F", "c",        // custom format (pg_restore)
                "-b",
                "--clean",
                "--if-exists",
                "--no-owner",
                "--no-privileges",
                dbName
        );

        // VERY IMPORTANT
        pb.environment().put("PGPASSWORD", "Anjenk132");

        // Write pg_dump output to file
        pb.redirectOutput(backupFile);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("pg_dump failed");
        }

        return backupFile;
    }
}
