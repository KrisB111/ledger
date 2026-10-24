package com.example.ledger.config;

import com.example.ledger.model.AccountEntity;
import com.example.ledger.model.TransactionType;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.LedgerRepository;
import com.example.ledger.service.LedgerService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("seed-data")
@Order(2)
public class SampleDataInitializer implements ApplicationRunner {
    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;
    private final LedgerService ledgerService;

    public SampleDataInitializer(
            AccountRepository accountRepository,
            LedgerRepository ledgerRepository,
            LedgerService ledgerService) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.ledgerService = ledgerService;
    }

    @Override
    public void run(ApplicationArguments args) {
        AccountEntity account = accountRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("Default account was not initialized."));
        if (!ledgerRepository.findByAccount_Id(account.getId()).isEmpty()) {
            return;
        }

        ledgerService.recordTransaction(account.getId(), TransactionType.DEPOSIT, new BigDecimal("100.00"));
        ledgerService.recordTransaction(account.getId(), TransactionType.WITHDRAWAL, new BigDecimal("20.50"));
        ledgerService.recordTransaction(account.getId(), TransactionType.DEPOSIT, new BigDecimal("25.00"));
    }
}
