package com.pos.posApps.Entity;

import com.pos.posApps.DTO.Enum.AttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "staff_attendance")
public class StaffAttendanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "staff_attendance_seq")
    @SequenceGenerator(
            name = "staff_attendance_seq",
            sequenceName = "staff_attendance_sequences",
            allocationSize = 1
    )
    @Column(name = "attendance_id")
    private Long attendanceId;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffEntity staffEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AttendanceStatus status;

    @Column(name = "keterangan")
    private String keterangan;

    @Column(name = "attendance_date")
    private LocalDate attendanceDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
