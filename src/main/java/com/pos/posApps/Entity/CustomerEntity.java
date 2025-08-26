package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;


@Entity
@Data
@Table(name = "customer")
public class CustomerEntity {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "name")
    private String name;

    //todo make sure gonna use this or not
//    @Column(name = "customer_level_id")
//    private String customerLevelid;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}
