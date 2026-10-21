package com.pos.posApps.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "insentif_payout")
public class InsentifPayoutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "insentif_payout_seq")
    @SequenceGenerator(name = "insentif_payout_seq", sequenceName = "insentif_payout_sequences", allocationSize = 1)
    @Column(name = "insentif_payout_id")
    private Long insentifPayoutId;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private StaffEntity staffEntity;

    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "amount")
    private BigDecimal amount;

    // Number of paid months whose insentif this payout settles
    @Column(name = "month_count")
    private int monthCount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
