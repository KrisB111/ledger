package com.example.ledger.service;

import com.example.ledger.dto.MonthlySummaryResponse;
import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.model.LedgerTransactionEntity;
import com.example.ledger.model.TransactionType;
import com.example.ledger.repository.LedgerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
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
            BigDecimal signedAmount = switch (transaction.type()) {
                case DEPOSIT -> transaction.amount();
                case WITHDRAWAL -> transaction.amount().negate();
            };
            balance = balance.add(signedAmount);
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

    public MonthlySummaryResponse getMonthlySummary(int year, int month) {
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be between 1 and 12.");
        }

        YearMonth targetMonth = YearMonth.of(year, month);
        ZoneId zone = ZoneId.systemDefault();
        Instant startInclusive = targetMonth.atDay(1).atStartOfDay(zone).toInstant();
        Instant endExclusive = targetMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        List<LedgerTransactionEntity> transactions = ledgerRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startInclusive, endExclusive);
        for (LedgerTransactionEntity entity : transactions) {
            LedgerTransaction transaction = entity.toDomain();

            MonthlyTotals totals = switch (transaction.type()) {
                case DEPOSIT -> new MonthlyTotals(transaction.amount(), BigDecimal.ZERO);
                case WITHDRAWAL -> new MonthlyTotals(BigDecimal.ZERO, transaction.amount());
            };
            income = income.add(totals.income());
            expenses = expenses.add(totals.expenses());
        }

        return new MonthlySummaryResponse(year, month, income, expenses, income.subtract(expenses));
    }

    private record MonthlyTotals(BigDecimal income, BigDecimal expenses) {
    }
}