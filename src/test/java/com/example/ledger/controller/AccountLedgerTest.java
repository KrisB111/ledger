package com.example.ledger.controller;

import com.example.ledger.model.AccountEntity;
import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.model.LedgerTransactionEntity;
import com.example.ledger.model.TransactionType;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.LedgerRepository;
import com.example.ledger.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AccountLedgerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private LedgerService ledgerService;

    private Long accountId;

    @BeforeEach
    void setUp() {
        ledgerRepository.deleteAll();
        accountRepository.deleteAll();
        accountId = accountRepository.save(new AccountEntity(BigDecimal.ZERO)).getId();
    }

    @Test
    void createsAccountAndKeepsItsTransactionsAndBalanceSeparate() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/accounts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(0.0))
                .andReturn();
        Number secondAccountIdValue = com.jayway.jsonpath.JsonPath
            .read(created.getResponse().getContentAsString(), "$.id");
        long secondAccountId = secondAccountIdValue.longValue();

        postTransaction(accountId, "DEPOSIT", "100.00");
        postTransaction(secondAccountId, "DEPOSIT", "25.00");
        postTransaction(accountId, "WITHDRAWAL", "20.50");

        mockMvc.perform(get("/api/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(79.5));
        mockMvc.perform(get("/api/accounts/{accountId}/balance", secondAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(25.0));
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void reportsStoredBalanceMismatch() throws Exception {
        ledgerRepository.save(new LedgerTransactionEntity(
                accountRepository.findById(accountId).orElseThrow(),
                TransactionType.DEPOSIT,
                new BigDecimal("10.00"),
                Instant.now()));

        mockMvc.perform(get("/api/accounts/{accountId}/reconciliation", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storedBalance").value(0.0))
                .andExpect(jsonPath("$.calculatedBalance").value(10.0))
                .andExpect(jsonPath("$.consistent").value(false));
    }

    @Test
    void databaseRejectsNegativeAccountBalance() {
        AccountEntity account = accountRepository.findById(accountId).orElseThrow();
        account.setBalance(new BigDecimal("-0.01"));

        assertThrows(DataIntegrityViolationException.class, () -> accountRepository.saveAndFlush(account));
    }

    @Test
    void returnsInsufficientFundsReasonInResponse() throws Exception {
        mockMvc.perform(post("/api/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"WITHDRAWAL\",\"amount\":1.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Insufficient funds."));
    }

    @Test
    void serializesConcurrentWithdrawalsWithoutMakingBalanceNegative() throws Exception {
        ledgerService.recordTransaction(accountId, TransactionType.DEPOSIT, new BigDecimal("5.00"));
        int requestCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> requests = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                requests.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        ledgerService.recordTransaction(accountId, TransactionType.WITHDRAWAL,
                                new BigDecimal("1.00"));
                        return true;
                    } catch (ResponseStatusException exception) {
                        return false;
                    }
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successfulWithdrawals = 0;
            for (Future<Boolean> request : requests) {
                if (request.get(15, TimeUnit.SECONDS)) {
                    successfulWithdrawals++;
                }
            }

            assertEquals(5, successfulWithdrawals);
            assertEquals(0, ledgerService.getBalance(accountId).compareTo(BigDecimal.ZERO));
            assertTrue(ledgerService.reconcileAccount(accountId).consistent());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private void postTransaction(Long targetAccountId, String type, String amount) throws Exception {
        mockMvc.perform(post("/api/accounts/{accountId}/transactions", targetAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"" + type + "\",\"amount\":" + amount + "}"))
                .andExpect(status().isCreated());
    }
}
