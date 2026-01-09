package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "data_center_log")
public class DataCenterLogEntity {

    @Column(name = "nama_file")
    private String namaFile;

    @Id
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

