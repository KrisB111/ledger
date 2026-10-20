package com.example.ledger.repository;

import com.example.ledger.model.LedgerTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerTransactionEntity, Long> {
	List<LedgerTransactionEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
			Instant startInclusive,
			Instant endExclusive);
}