package com.example.ledger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_transactions")
public class LedgerTransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerTransactionEntity() {
    }

    public LedgerTransactionEntity(TransactionType type, BigDecimal amount, Instant createdAt) {
        this.type = type;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public static LedgerTransactionEntity fromDomain(LedgerTransaction transaction) {
        return new LedgerTransactionEntity(transaction.type(), transaction.amount(), transaction.createdAt());
    }

    public LedgerTransaction toDomain() {
        return new LedgerTransaction(type, amount, createdAt);
    }

    public Long getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
