package com.pos.posApps.ControllerRest;

import com.pos.posApps.Service.DataCenterService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api")
@AllArgsConstructor

public class RestControllerDataCenter {
    private final DataCenterService dataCenterService;

    @GetMapping(value = "/backup", produces = "application/sql")
    public ResponseEntity<Resource> backup() throws Exception {
        File file = dataCenterService.backupDatabase("postgres");
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/restore")
    public ResponseEntity<String> restoreDatabase(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        //Transfer to tempFile because pg_restore can't read directly the Multipart File
        File tempFile = File.createTempFile("restore-", ".dump");
        file.transferTo(tempFile);
        dataCenterService.restoreDatabase(tempFile);
        return ResponseEntity.ok("Database berhasil dipulihkan! Harap melakukan login ulang");
    }
}
