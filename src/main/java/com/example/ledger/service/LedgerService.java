package com.example.ledger.service;

import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.model.TransactionType;
import com.example.ledger.repository.LedgerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
}