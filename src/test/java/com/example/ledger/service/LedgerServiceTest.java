package com.example.ledger.service;

import com.example.ledger.mapper.LedgerTransactionMapper;
import com.example.ledger.model.AccountEntity;
import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.model.LedgerTransactionEntity;
import com.example.ledger.model.TransactionType;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {
    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private AccountRepository accountRepository;

    private LedgerService ledgerService;
    private AccountEntity account;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(ledgerRepository, accountRepository, new LedgerTransactionMapper());
        account = new AccountEntity(BigDecimal.ZERO);
    }

    @Test
    void recordsDepositAndUpdatesStoredAccountBalance() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(ledgerRepository.save(any(LedgerTransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LedgerTransaction result = ledgerService.recordTransaction(
                1L, TransactionType.DEPOSIT, new BigDecimal("12.50"));

        assertEquals(new BigDecimal("12.50"), account.getBalance());
        assertEquals(TransactionType.DEPOSIT, result.type());
        assertEquals(new BigDecimal("12.50"), result.amount());

        ArgumentCaptor<LedgerTransactionEntity> transactionCaptor = ArgumentCaptor.forClass(LedgerTransactionEntity.class);
        verify(ledgerRepository).save(transactionCaptor.capture());
        assertEquals(account, transactionCaptor.getValue().getAccount());
    }

    @Test
    void rejectsWithdrawalWhenAccountDoesNotHaveEnoughFunds() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ledgerService.recordTransaction(1L, TransactionType.WITHDRAWAL, new BigDecimal("1.00")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Insufficient funds.", exception.getReason());
        assertEquals(new BigDecimal("0.00"), account.getBalance());
        verify(ledgerRepository, never()).save(any(LedgerTransactionEntity.class));
    }

    @Test
    void calculatesMonthlySummaryFromTransactionsReturnedByRepository() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(ledgerRepository.findByAccount_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(1L), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        new LedgerTransactionEntity(account, TransactionType.DEPOSIT,
                                new BigDecimal("80.00"), Instant.parse("2026-10-10T12:00:00Z")),
                        new LedgerTransactionEntity(account, TransactionType.WITHDRAWAL,
                                new BigDecimal("15.25"), Instant.parse("2026-10-11T12:00:00Z"))));

        var summary = ledgerService.getMonthlySummary(1L, 2026, 10);

        assertEquals(new BigDecimal("80.00"), summary.income());
        assertEquals(new BigDecimal("15.25"), summary.expenses());
        assertEquals(new BigDecimal("64.75"), summary.net());
        verify(ledgerRepository).findByAccount_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(1L), any(Instant.class), any(Instant.class));
    }
}
