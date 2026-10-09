package com.example.ledger.model;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerTransaction(
        TransactionType type,
        BigDecimal amount,
        Instant createdAt) {
}