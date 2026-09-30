package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

//gambar = Sales
@Entity
@Data
@Table(name = "transaction")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_seq")
    @SequenceGenerator(name = "transaction_seq", sequenceName = "transaction_sequences", allocationSize = 1)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity clientEntity;

    @Column(name = "transaction_number")
    private String transactionNumber;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerEntity customerEntity;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @Column(name = "is_cash")
    private boolean isCash = true;

    @Column(name = "is_paid")
    private boolean isPaid = true;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "payment_method_name")
    private String paymentMethodName;

    @Column(name = "payment_method_type")
    private String paymentMethodType;

    @Column(name = "payment_method_rekening")
    private String paymentMethodRekening;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "transactionEntity")
    private List<TransactionDetailEntity> transactionDetailEntities;

    @OneToOne
    @JoinColumn(name = "created_by")
    private AccountEntity accountEntity;
}
