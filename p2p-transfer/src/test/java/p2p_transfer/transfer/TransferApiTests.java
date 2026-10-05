package p2p_transfer.transfer;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import p2p_transfer.account.Account;
import p2p_transfer.common.Iban;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.user.User;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static p2p_transfer.support.Money.eq;
import static p2p_transfer.support.TestData.as;

class TransferApiTests extends IntegrationTest {

    User alice;
    User bob;
    User carol;
    Account aliceAcc;
    Account bobAcc;

    @BeforeEach
    void setUp() {
        alice = data.user("Alice");
        bob = data.user("Bob");
        carol = data.user("Carol");
        aliceAcc = data.account(alice, "1000.00");
        bobAcc = data.account(bob, "500.00");
    }

    @Test
    void successfulTransferMovesMoneyAndReturnsSenderView() throws Exception {
        transfer(alice, aliceAcc, bobAcc.getIban(), "150.25", null)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andExpect(jsonPath("$.direction").value("OUTGOING"))
                .andExpect(jsonPath("$.amount", eq("150.25")))
                .andExpect(jsonPath("$.balanceAfter", eq("849.75")))
                .andExpect(jsonPath("$.counterparty.name").value("Bob Test"))
                .andExpect(jsonPath("$.category").value("RENT"))
                .andExpect(jsonPath("$.reference", startsWith("BQ")));

        assertBalances("849.75", "650.25");

        mvc.perform(get("/api/transactions").with(as(bob)))
                .andExpect(jsonPath("$.content[0].direction").value("INCOMING"))
                .andExpect(jsonPath("$.content[0].balanceAfter", eq("650.25")))
                .andExpect(jsonPath("$.content[0].counterparty.name").value("Alice Test"));
    }

    @Test
    void ibanMayBeEnteredWithSpacesAndLowercase() throws Exception {
        String messy = Iban.format(bobAcc.getIban()).toLowerCase();
        transfer(alice, aliceAcc, messy, "10", null).andExpect(status().isCreated());
    }

    @Test
    void insufficientFundsIsRejectedWithoutSideEffects() throws Exception {
        transfer(alice, aliceAcc, bobAcc.getIban(), "1000.01", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
        assertBalances("1000.00", "500.00");
        assertThat(jdbc.queryForObject("select count(*) from transactions", Long.class)).isZero();
    }

    @Test
    void amountMustBePositiveWithAtMostTwoDecimals() throws Exception {
        for (String amount : new String[]{"-50", "0", "0.001"}) {
            transfer(alice, aliceAcc, bobAcc.getIban(), amount, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.amount").exists());
        }
        assertBalances("1000.00", "500.00");
    }

    @Test
    void cannotSendFromSomeoneElsesAccount() throws Exception {
        transfer(carol, aliceAcc, bobAcc.getIban(), "10", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        assertBalances("1000.00", "500.00");
    }

    @Test
    void unknownIbanAndSameAccountAreRejected() throws Exception {
        transfer(alice, aliceAcc, "TR000000000000000000000000", "10", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IBAN_NOT_FOUND"));
        transfer(alice, aliceAcc, aliceAcc.getIban(), "10", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SAME_ACCOUNT"));
    }

    @Test
    void currenciesMustMatch() throws Exception {
        Account usd = data.account(bob, "100", "USD");
        transfer(alice, aliceAcc, usd.getIban(), "10", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));
    }

    @Test
    void dailyLimitAppliesToOthersButNotToOwnAccounts() throws Exception {
        Account rich = data.account(alice, "200000.00");
        Account aliceSavings = data.account(alice, "0");

        transfer(alice, rich, bobAcc.getIban(), "30000", null).andExpect(status().isCreated());
        transfer(alice, rich, bobAcc.getIban(), "25000", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DAILY_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.remaining", eq("20000")));

        transfer(alice, rich, aliceSavings.getIban(), "100000", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.internal").value(true));
    }

    @Test
    void retryWithSameIdempotencyKeyDoesNotChargeTwice() throws Exception {
        String key = "3f1c2a7e-0b5d-4e8a-9c61-1d2e3f4a5b6c";
        String reference = JsonPath.read(
                transfer(alice, aliceAcc, bobAcc.getIban(), "100", key)
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(), "$.reference");

        transfer(alice, aliceAcc, bobAcc.getIban(), "100", key)
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.reference").value(reference));

        assertBalances("900.00", "600.00");

        transfer(alice, aliceAcc, bobAcc.getIban(), "999", key)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void transactionsAreVisibleOnlyToParticipants() throws Exception {
        String body = transfer(alice, aliceAcc, bobAcc.getIban(), "10", null)
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/transactions").with(as(carol)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/transactions/{id}", id).with(as(carol)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
        mvc.perform(get("/api/transactions/{id}", id).with(as(bob)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/transactions").param("accountId", aliceAcc.getId().toString()).with(as(carol)))
                .andExpect(status().isNotFound());
    }

    @Test
    void historySupportsSearchDirectionAndPaging() throws Exception {
        transfer(alice, aliceAcc, bobAcc.getIban(), "10", null);
        transfer(alice, aliceAcc, bobAcc.getIban(), "20", null);
        transfer(bob, bobAcc, aliceAcc.getIban(), "5", null);

        mvc.perform(get("/api/transactions").param("direction", "OUTGOING").with(as(alice)))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/transactions").param("direction", "INCOMING").with(as(alice)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amount", eq("5")));
        mvc.perform(get("/api/transactions").param("q", "kira").with(as(alice)))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/transactions").param("q", "50%_").with(as(alice)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/transactions").param("size", "2").param("page", "1").with(as(alice)))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                // en yeni önce: sayfa 2'deki tek kayıt ilk yapılan transfer
                .andExpect(jsonPath("$.content[0].amount", eq("10")));
    }

    @Test
    void csvExportContainsFilteredRows() throws Exception {
        transfer(alice, aliceAcc, bobAcc.getIban(), "42.50", null);
        byte[] csv = mvc.perform(get("/api/transactions/export").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", startsWith("attachment")))
                .andReturn().getResponse().getContentAsByteArray();
        String text = new String(csv, StandardCharsets.UTF_8);
        assertThat(text).startsWith("﻿").contains("Bob Test").contains("-42,50");
    }

    private ResultActions transfer(User as, Account from, String toIban, String amount, String idempotencyKey) throws Exception {
        var request = post("/api/transactions/transfer").with(as(as)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fromAccountId":%d,"toIban":"%s","amount":%s,"description":"Kira","category":"RENT"}"""
                        .formatted(from.getId(), toIban, amount));
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mvc.perform(request);
    }

    private void assertBalances(String alice, String bob) {
        assertThat(data.balance(aliceAcc)).isEqualByComparingTo(alice);
        assertThat(data.balance(bobAcc)).isEqualByComparingTo(bob);
    }
}
