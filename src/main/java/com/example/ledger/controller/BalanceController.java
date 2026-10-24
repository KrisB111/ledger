package com.example.ledger.controller;

import com.example.ledger.dto.BalanceResponse;
import com.example.ledger.dto.MonthlySummaryResponse;
import com.example.ledger.model.LedgerCurrency;
import com.example.ledger.service.LedgerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BalanceController {
    private final LedgerService ledgerService;

    public BalanceController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/balance")
    public BalanceResponse getBalance() {
        return new BalanceResponse(ledgerService.getBalance(), LedgerCurrency.CODE);
    }

    @GetMapping("/reports/monthly")
    public MonthlySummaryResponse getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month) {
        return ledgerService.getMonthlySummary(year, month);
    }
}