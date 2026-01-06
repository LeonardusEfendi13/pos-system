package com.pos.posApps.ControllerRest;

import com.pos.posApps.Service.PostgresBackupService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/api/backup")
@AllArgsConstructor

public class RestControllerBackup {
    private final PostgresBackupService backupService;

    @GetMapping(value = "/database", produces = "application/sql")
    public ResponseEntity<Resource> backup() throws Exception {

        File file = backupService.backupDatabase("postgres");

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
