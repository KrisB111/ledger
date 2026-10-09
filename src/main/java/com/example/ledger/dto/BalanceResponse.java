package com.example.ledger.dto;

import java.math.BigDecimal;

public record BalanceResponse(BigDecimal balance, String currency) {
}