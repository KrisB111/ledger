package com.example.ledger.service;

import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.model.TransactionType;
import com.example.ledger.repository.LedgerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

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

        for (LedgerTransaction transaction : ledgerRepository.findAll()) {
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
        ledgerRepository.save(transaction);
        return transaction;
    }

    public List<LedgerTransaction> getTransactions() {
        return ledgerRepository.findAll();
    }
}