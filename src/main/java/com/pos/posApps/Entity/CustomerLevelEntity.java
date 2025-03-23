package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

//todo make sure gonna use this or not
@Entity
@Data
@Table(name = "customer_level")
public class CustomerLevelEntity {

    @Id
    private String customerLevelId;

    @Column(name = "title")
    private String title;

    @Column(name = "minimal_spending")
    private BigDecimal minimalSpending;

    @Column(name = "maximal_spending")
    private BigDecimal maximalSpending;
}
