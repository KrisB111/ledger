package com.example.ledger.dto;

import java.math.BigDecimal;

public record MonthlySummaryResponse(
        int year,
        int month,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal net) {
}
