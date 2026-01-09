package com.pos.posApps.Service;

import com.pos.posApps.Entity.DataCenterLogEntity;
import com.pos.posApps.Repository.DataCenterLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.pos.posApps.Constants.Constant.PG_DUMP;
import static com.pos.posApps.Constants.Constant.PG_RESTORE;
import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class DataCenterService {

    @Autowired
    private DataCenterLogRepository dataCenterLogRepository;

    public List<DataCenterLogEntity> getLogData(){
        return dataCenterLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public File backupDatabase(String dbName)
            throws IOException, InterruptedException {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);

        // Use .dump or .backup (NOT .sql)
        String fileName = "backup-ANUGRAH MOTOR-" + timestamp + ".sql";

        File backupFile = new File(
                System.getProperty("java.io.tmpdir"),
                fileName
        );

        ProcessBuilder pb = new ProcessBuilder(
                PG_DUMP,
                "-h", "localhost",
                "-p", "5432",
                "-U", "postgres",
                "-F", "c",              // CUSTOM format (pg_restore)
                "-b",
                "--no-owner",
                "--no-privileges",
                dbName
        );

        // Password from env / config
        pb.environment().put("PGPASSWORD", "Anjenk132");

        // Write dump to file
        pb.redirectOutput(backupFile);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Backup timeout");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("pg_dump failed with exit code " + exitCode);
        }

        //Take a record
        DataCenterLogEntity dataCenterLogEntity = new DataCenterLogEntity();
        dataCenterLogEntity.setNamaFile(backupFile.getName());
        dataCenterLogEntity.setCreatedAt(getCurrentTimestamp());
        dataCenterLogRepository.save(dataCenterLogEntity);
        return backupFile;
    }

    public void restoreDatabase(
            File dumpFile
    ) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                PG_RESTORE,
                "-h", "localhost",
                "-p", "5432",
                "-U", "postgres",
                "--clean",
                "--if-exists",
                "--no-owner",
                "--no-privileges",
                "-d", "dev",
                dumpFile.getAbsolutePath()
        );

        // Avoid password prompt
        pb.environment().put("PGPASSWORD", "Anjenk132");

        // Show logs in console
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("pg_restore failed with exit code " + exitCode);
        }
    }
}
