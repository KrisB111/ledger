package com.example.ledger.dto;

import com.example.ledger.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(TransactionType type, BigDecimal amount, Instant createdAt) {
}