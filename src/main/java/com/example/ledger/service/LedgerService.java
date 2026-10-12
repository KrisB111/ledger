package com.example.ledger.service;

import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.model.LedgerTransactionEntity;
import com.example.ledger.model.TransactionType;
import com.example.ledger.repository.LedgerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class LedgerService {
    private final LedgerRepository ledgerRepository;

    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public BigDecimal getBalance() {
        BigDecimal balance = new BigDecimal("0.00");

        for (LedgerTransaction transaction : getTransactions()) {
            if (transaction.type() == TransactionType.DEPOSIT) {
                balance = balance.add(transaction.amount());
            } else {
                balance = balance.subtract(transaction.amount());
            }
        }

        return balance;
    }

    public LedgerTransaction recordTransaction(TransactionType type, BigDecimal amount) {
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction type is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount cannot have fractions of a cent.");
        }
        if (type == TransactionType.WITHDRAWAL && getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds.");
        }

        LedgerTransaction transaction = new LedgerTransaction(type, amount.setScale(2), Instant.now());
        LedgerTransactionEntity saved = ledgerRepository.save(LedgerTransactionEntity.fromDomain(transaction));
        return saved.toDomain();
    }

    public List<LedgerTransaction> getTransactions() {
        return ledgerRepository.findAll().stream()
                .map(LedgerTransactionEntity::toDomain)
                .toList();
    }
}