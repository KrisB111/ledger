package com.example.ledger.repository;

import com.example.ledger.model.LedgerTransaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class InMemoryLedgerRepository implements LedgerRepository {
    private final List<LedgerTransaction> transactions = new ArrayList<>();

    @Override
    public List<LedgerTransaction> findAll() {
        return List.copyOf(transactions);
    }

    @Override
    public void save(LedgerTransaction transaction) {
        transactions.add(transaction);
    }
}