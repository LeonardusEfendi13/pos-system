package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

//todo make sure gonna use this or not
@Entity
@Data
public class CustomerLevelEntity {

    @Id
    private String customerLevelId;

    @Column(name = "title")
    private String title;

    @Column(name = "customer_level_id")
    private String customerLevelid;

    @Column(name = "minimal_spending")
    private BigDecimal minimalSpending;

    @Column(name = "maximal_spending")
    private BigDecimal maximalSpending;
}
