package com.example.ledger.controller;

import com.example.ledger.model.LedgerTransactionEntity;
import com.example.ledger.model.TransactionType;
import com.example.ledger.repository.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class BalanceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LedgerRepository ledgerRepository;

    @BeforeEach
    void setUp() {
        ledgerRepository.deleteAll();
    }

    @Test
    void returnsZeroBalanceForNewLedger() throws Exception {
        mockMvc.perform(get("/api/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

            @Test
            void recordsTransactionsReturnsHistoryAndRejectsInvalidAmounts() throws Exception {
            mockMvc.perform(post("/api/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"DEPOSIT\",\"amount\":25.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(25.0))
                .andExpect(jsonPath("$.createdAt").exists());

            mockMvc.perform(post("/api/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"WITHDRAWAL\",\"amount\":5.25}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"));

            mockMvc.perform(get("/api/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(19.75));

            mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[1].type").value("WITHDRAWAL"));

            mockMvc.perform(post("/api/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"DEPOSIT\",\"amount\":0}"))
                .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"WITHDRAWAL\",\"amount\":100}"))
                .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"DEPOSIT\",\"amount\":1.001}"))
                .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"DEPOSIT\",\"amount\":1.000}"))
                .andExpect(status().isBadRequest());

            mockMvc.perform(get("/api/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(19.75));
            }

    @Test
    void returnsMonthlySummaryForCurrentMonth() throws Exception {
        int year = YearMonth.now().getYear();
        int month = YearMonth.now().getMonthValue();

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"DEPOSIT\",\"amount\":100.00}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"WITHDRAWAL\",\"amount\":20.50}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reports/monthly")
                .param("year", String.valueOf(year))
                .param("month", String.valueOf(month)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.year").value(year))
            .andExpect(jsonPath("$.month").value(month))
            .andExpect(jsonPath("$.income").value(100.0))
            .andExpect(jsonPath("$.expenses").value(20.5))
            .andExpect(jsonPath("$.net").value(79.5));
    }

    @Test
    void repositoryLoadsOnlyTransactionsInsideRequestedTimeRange() {
        YearMonth month = YearMonth.of(2026, 10);
        Instant start = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        ledgerRepository.save(new LedgerTransactionEntity(
                TransactionType.DEPOSIT, new BigDecimal("1.00"), start.minusSeconds(1)));
        ledgerRepository.save(new LedgerTransactionEntity(
                TransactionType.DEPOSIT, new BigDecimal("2.00"), start));
        ledgerRepository.save(new LedgerTransactionEntity(
                TransactionType.DEPOSIT, new BigDecimal("3.00"), end.minusSeconds(1)));
        ledgerRepository.save(new LedgerTransactionEntity(
                TransactionType.DEPOSIT, new BigDecimal("4.00"), end));

        List<BigDecimal> amounts = ledgerRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)
                .stream()
                .map(LedgerTransactionEntity::getAmount)
                .toList();

        assertEquals(List.of(new BigDecimal("2.00"), new BigDecimal("3.00")), amounts);
    }
}