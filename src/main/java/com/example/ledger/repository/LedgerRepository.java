package com.example.ledger.repository;

import com.example.ledger.model.LedgerTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerTransactionEntity, Long> {
	List<LedgerTransactionEntity> findByAccount_IdOrderByCreatedAtAsc(Long accountId);

	List<LedgerTransactionEntity> findByAccount_Id(Long accountId);

	List<LedgerTransactionEntity> findByAccount_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
			Long accountId,
			Instant startInclusive,
			Instant endExclusive);
}