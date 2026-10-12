package com.example.ledger.repository;

import com.example.ledger.model.LedgerTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<LedgerTransactionEntity, Long> {
}