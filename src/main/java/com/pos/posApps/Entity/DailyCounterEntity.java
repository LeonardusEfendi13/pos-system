package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "daily_counter")
public class DailyCounterEntity {

    @Id
    @Column(name = "counter_date")
    private LocalDate counterDate;

    @Column(name = "last_counter", nullable = false)
    private Long lastCounter;
}
