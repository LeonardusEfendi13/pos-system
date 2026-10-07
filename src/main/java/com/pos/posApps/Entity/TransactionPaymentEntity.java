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
@Table(name = "transaction_payment")
public class TransactionPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_payment_seq")
    @SequenceGenerator(
            name = "transaction_payment_seq",
            sequenceName = "transaction_payment_sequences",
            allocationSize = 1
    )
    @Column(name = "transaction_payment_id")
    private Long transactionPaymentId;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private TransactionEntity transactionEntity;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "payment_method_name")
    private String paymentMethodName;

    @Column(name = "payment_method_type")
    private String paymentMethodType;

    @Column(name = "payment_method_rekening")
    private String paymentMethodRekening;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
