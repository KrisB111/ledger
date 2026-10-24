package com.example.ledger.controller;

import com.example.ledger.dto.TransactionRequest;
import com.example.ledger.dto.TransactionResponse;
import com.example.ledger.mapper.TransactionResponseMapper;
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
    private final TransactionResponseMapper transactionResponseMapper;

    public TransactionController(
            LedgerService ledgerService,
            TransactionResponseMapper transactionResponseMapper) {
        this.ledgerService = ledgerService;
        this.transactionResponseMapper = transactionResponseMapper;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionResponseMapper.toResponse(
                        ledgerService.recordTransaction(request.type(), request.amount())));
    }

    @GetMapping
    public List<TransactionResponse> getTransactions() {
        return ledgerService.getTransactions().stream()
                .map(transactionResponseMapper::toResponse)
                .toList();
    }
}