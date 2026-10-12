package com.example.ledger.dto;

import com.example.ledger.model.TransactionType;

import java.math.BigDecimal;

public record TransactionRequest(TransactionType type, BigDecimal amount) {
}