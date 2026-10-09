package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "client_role",
        uniqueConstraints = @UniqueConstraint(name = "uq_client_role_name", columnNames = {"client_id", "name"})
)
public class ClientRoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_role_seq")
    @SequenceGenerator(name = "client_role_seq", sequenceName = "client_role_sequences", allocationSize = 1)
    @Column(name = "client_role_id")
    private Long clientRoleId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "seeded", nullable = false)
    private boolean seeded;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
