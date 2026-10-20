package com.example.ledger.service;

import com.example.ledger.dto.MonthlySummaryResponse;
import com.example.ledger.dto.ReconciliationResponse;
import com.example.ledger.mapper.LedgerTransactionMapper;
import com.example.ledger.model.AccountEntity;
import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.model.LedgerTransactionEntity;
import com.example.ledger.model.TransactionType;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.LedgerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
public class LedgerService {
    private final LedgerRepository ledgerRepository;
    private final AccountRepository accountRepository;
    private final LedgerTransactionMapper transactionMapper;

    public LedgerService(
            LedgerRepository ledgerRepository,
            AccountRepository accountRepository,
            LedgerTransactionMapper transactionMapper) {
        this.ledgerRepository = ledgerRepository;
        this.accountRepository = accountRepository;
        this.transactionMapper = transactionMapper;
    }

    public BigDecimal getBalance() {
        return getBalance(getDefaultAccountId());
    }

    public BigDecimal getBalance(Long accountId) {
        return requireAccount(accountId).getBalance();
    }

    @Transactional
    public LedgerTransaction recordTransaction(TransactionType type, BigDecimal amount) {
        return recordTransaction(getDefaultAccountId(), type, amount);
    }

    @Transactional
    public LedgerTransaction recordTransaction(Long accountId, TransactionType type, BigDecimal amount) {
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction type is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount cannot have fractions of a cent.");
        }

        AccountEntity account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
        if (type == TransactionType.WITHDRAWAL && account.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds.");
        }

        BigDecimal transactionAmount = amount.setScale(2);
        BigDecimal newBalance = switch (type) {
            case DEPOSIT -> account.getBalance().add(transactionAmount);
            case WITHDRAWAL -> account.getBalance().subtract(transactionAmount);
        };
        account.setBalance(newBalance);

        LedgerTransaction transaction = new LedgerTransaction(type, transactionAmount, Instant.now());
        LedgerTransactionEntity saved = ledgerRepository.save(transactionMapper.toEntity(account, transaction));
        return transactionMapper.toDomain(saved);
    }

    public List<LedgerTransaction> getTransactions() {
        return getTransactions(getDefaultAccountId());
    }

    public List<LedgerTransaction> getTransactions(Long accountId) {
        requireAccount(accountId);
        return ledgerRepository.findByAccount_IdOrderByCreatedAtAsc(accountId).stream()
                .map(transactionMapper::toDomain)
                .toList();
    }

    public MonthlySummaryResponse getMonthlySummary(int year, int month) {
        return getMonthlySummary(getDefaultAccountId(), year, month);
    }

    public MonthlySummaryResponse getMonthlySummary(Long accountId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be between 1 and 12.");
        }
        requireAccount(accountId);

        YearMonth targetMonth = YearMonth.of(year, month);
        ZoneId zone = ZoneId.systemDefault();
        Instant startInclusive = targetMonth.atDay(1).atStartOfDay(zone).toInstant();
        Instant endExclusive = targetMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        List<LedgerTransactionEntity> transactions = ledgerRepository
            .findByAccount_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                accountId, startInclusive, endExclusive);
        for (LedgerTransactionEntity entity : transactions) {
            LedgerTransaction transaction = transactionMapper.toDomain(entity);

            MonthlyTotals totals = switch (transaction.type()) {
                case DEPOSIT -> new MonthlyTotals(transaction.amount(), BigDecimal.ZERO);
                case WITHDRAWAL -> new MonthlyTotals(BigDecimal.ZERO, transaction.amount());
            };
            income = income.add(totals.income());
            expenses = expenses.add(totals.expenses());
        }

        return new MonthlySummaryResponse(year, month, income, expenses, income.subtract(expenses));
    }

    public ReconciliationResponse reconcileAccount(Long accountId) {
        AccountEntity account = requireAccount(accountId);
        BigDecimal calculatedBalance = BigDecimal.ZERO;

        for (LedgerTransactionEntity entity : ledgerRepository.findByAccount_Id(accountId)) {
            LedgerTransaction transaction = transactionMapper.toDomain(entity);
            BigDecimal signedAmount = switch (transaction.type()) {
                case DEPOSIT -> transaction.amount();
                case WITHDRAWAL -> transaction.amount().negate();
            };
            calculatedBalance = calculatedBalance.add(signedAmount);
        }

        return new ReconciliationResponse(
                accountId,
                account.getBalance(),
                calculatedBalance,
                account.getBalance().compareTo(calculatedBalance) == 0);
    }

    @Transactional
    public AccountEntity createAccount() {
        return accountRepository.save(new AccountEntity(BigDecimal.ZERO));
    }

    public AccountEntity requireAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));
    }

    private Long getDefaultAccountId() {
        return accountRepository.findFirstByOrderByIdAsc()
                .map(AccountEntity::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No ledger account exists."));
    }

    private record MonthlyTotals(BigDecimal income, BigDecimal expenses) {
    }
}