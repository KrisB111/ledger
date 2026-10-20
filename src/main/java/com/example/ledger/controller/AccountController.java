package com.example.ledger.controller;

import com.example.ledger.dto.AccountResponse;
import com.example.ledger.dto.BalanceResponse;
import com.example.ledger.dto.MonthlySummaryResponse;
import com.example.ledger.dto.ReconciliationResponse;
import com.example.ledger.dto.TransactionRequest;
import com.example.ledger.dto.TransactionResponse;
import com.example.ledger.model.AccountEntity;
import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.service.LedgerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final LedgerService ledgerService;

    public AccountController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount() {
        AccountEntity account = ledgerService.createAccount();
        return ResponseEntity.status(HttpStatus.CREATED).body(toAccountResponse(account));
    }

    @GetMapping("/{accountId}/balance")
    public BalanceResponse getBalance(@PathVariable Long accountId) {
        return new BalanceResponse(ledgerService.getBalance(accountId), "EUR");
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable Long accountId,
            @RequestBody TransactionRequest request) {
        LedgerTransaction transaction = ledgerService.recordTransaction(
                accountId, request.type(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(toTransactionResponse(transaction));
    }

    @GetMapping("/{accountId}/transactions")
    public List<TransactionResponse> getTransactions(@PathVariable Long accountId) {
        return ledgerService.getTransactions(accountId).stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @GetMapping("/{accountId}/reports/monthly")
    public MonthlySummaryResponse getMonthlySummary(
            @PathVariable Long accountId,
            @RequestParam int year,
            @RequestParam int month) {
        return ledgerService.getMonthlySummary(accountId, year, month);
    }

    @GetMapping("/{accountId}/reconciliation")
    public ReconciliationResponse reconcile(@PathVariable Long accountId) {
        return ledgerService.reconcileAccount(accountId);
    }

    private AccountResponse toAccountResponse(AccountEntity account) {
        return new AccountResponse(account.getId(), account.getBalance(), "EUR");
    }

    private TransactionResponse toTransactionResponse(LedgerTransaction transaction) {
        return new TransactionResponse(transaction.type(), transaction.amount(), transaction.createdAt());
    }
}