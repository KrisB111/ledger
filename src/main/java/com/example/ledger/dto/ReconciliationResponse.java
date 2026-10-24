package com.example.ledger.dto;

import java.math.BigDecimal;

public record ReconciliationResponse(
        Long accountId,
        BigDecimal storedBalance,
        BigDecimal calculatedBalance,
        boolean consistent) {
}