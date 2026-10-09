package com.example.ledger.repository;

import com.example.ledger.model.LedgerTransaction;

import java.util.List;

public interface LedgerRepository {
    List<LedgerTransaction> findAll();

    void save(LedgerTransaction transaction);
}