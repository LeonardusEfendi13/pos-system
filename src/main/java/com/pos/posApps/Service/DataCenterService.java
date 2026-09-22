package com.pos.posApps.Service;

import com.pos.posApps.Entity.DataCenterLogEntity;
import com.pos.posApps.Repository.DataCenterLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    public List<DataCenterLogEntity> getLogData(){
        return dataCenterLogRepository.findAllByOrderByCreatedAtDesc();
    }

    private record PgConnection(String host, String port, String database) {}

    private PgConnection resolvePgConnection() {
        String normalized = datasourceUrl.replaceFirst("^jdbc:postgresql://", "");
        String withoutQuery = normalized.split("\\?", 2)[0];
        String[] hostAndDb = withoutQuery.split("/", 2);
        String[] hostParts = hostAndDb[0].split(":", 2);
        String host = hostParts[0];
        String port = hostParts.length > 1 ? hostParts[1] : "5432";
        String database = hostAndDb.length > 1 && !hostAndDb[1].isBlank()
                ? hostAndDb[1]
                : "postgres";
        return new PgConnection(host, port, database);
    }

    public File backupDatabase()
            throws IOException, InterruptedException {
        PgConnection connection = resolvePgConnection();

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
                "-h", connection.host(),
                "-p", connection.port(),
                "-U", datasourceUsername,
                "-F", "c",              // CUSTOM format (pg_restore)
                "-b",
                "--no-owner",
                "--no-privileges",
                connection.database()
        );

        pb.environment().put("PGPASSWORD", datasourcePassword);

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
        PgConnection connection = resolvePgConnection();

        ProcessBuilder pb = new ProcessBuilder(
                PG_RESTORE,
                "-h", connection.host(),
                "-p", connection.port(),
                "-U", datasourceUsername,
                "--clean",
                "--if-exists",
                "--no-owner",
                "--no-privileges",
                "-d", connection.database(),
                dumpFile.getAbsolutePath()
        );

        pb.environment().put("PGPASSWORD", datasourcePassword);

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
