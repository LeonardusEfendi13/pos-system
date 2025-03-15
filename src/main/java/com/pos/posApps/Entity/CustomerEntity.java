package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.sql.Timestamp;



@Entity
@Data
public class CustomerEntity {

    @Id
    private String customerId;

    @Column(name = "name")
    private String name;

    //todo make sure gonna use this or not
    @Column(name = "customer_level_id")
    private String customerLevelid;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
