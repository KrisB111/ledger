package com.example.ledger.config;

import com.example.ledger.model.AccountEntity;
import com.example.ledger.repository.AccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
public class DefaultAccountInitializer implements ApplicationRunner {
    private final AccountRepository accountRepository;

    public DefaultAccountInitializer(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accountRepository.count() == 0) {
            accountRepository.save(new AccountEntity(BigDecimal.ZERO));
        }
    }
}