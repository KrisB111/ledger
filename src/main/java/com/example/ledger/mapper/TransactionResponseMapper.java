package com.example.ledger.mapper;

import com.example.ledger.dto.TransactionResponse;
import com.example.ledger.model.LedgerTransaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionResponseMapper {
    public TransactionResponse toResponse(LedgerTransaction transaction) {
        return new TransactionResponse(transaction.type(), transaction.amount(), transaction.createdAt());
    }
}
