package com.example.ledger.controller;

import com.example.ledger.dto.TransactionRequest;
import com.example.ledger.dto.TransactionResponse;
import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.service.LedgerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final LedgerService ledgerService;

    public TransactionController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request) {
        LedgerTransaction transaction = ledgerService.recordTransaction(request.type(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(transaction));
    }

    @GetMapping
    public List<TransactionResponse> getTransactions() {
        return ledgerService.getTransactions().stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(LedgerTransaction transaction) {
        return new TransactionResponse(transaction.type(), transaction.amount(), transaction.createdAt());
    }
}