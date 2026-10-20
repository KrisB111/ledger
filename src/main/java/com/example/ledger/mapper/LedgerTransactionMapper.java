package com.example.ledger.mapper;

import com.example.ledger.model.AccountEntity;
import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.model.LedgerTransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class LedgerTransactionMapper {
    public LedgerTransactionEntity toEntity(AccountEntity account, LedgerTransaction transaction) {
        return new LedgerTransactionEntity(
                account,
                transaction.type(),
                transaction.amount(),
                transaction.createdAt());
    }

    public LedgerTransaction toDomain(LedgerTransactionEntity entity) {
        return new LedgerTransaction(entity.getType(), entity.getAmount(), entity.getCreatedAt());
    }
}
