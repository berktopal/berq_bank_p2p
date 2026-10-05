package p2p_transfer.request;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import p2p_transfer.account.Account;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static p2p_transfer.support.Money.eq;
import static p2p_transfer.support.TestData.as;

class PaymentRequestApiTests extends IntegrationTest {

    @Autowired
    PaymentRequestService service;

    User ada;
    User bob;
    User eve;
    Account adaAcc;
    Account bobAcc;

    @BeforeEach
    void setUp() {
        ada = data.user("Ada");
        bob = data.user("Bob");
        eve = data.user("Eve");
        adaAcc = data.account(ada, "100");
        bobAcc = data.account(bob, "500");
    }

    @Test
    void requesterAsksPayerPaysAndBothAreNotified() throws Exception {
        Integer id = create(ada, adaAcc, bobAcc.getIban(), "120.50")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direction").value("OUTGOING"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.counterpartyName").value("Bob T***"))
                .andExpect(jsonPath("$.currency").value("TRY"))
                .andReturn().getResponse().getContentAsString().transform(b -> JsonPath.read(b, "$.id"));

        // Ödeyen, isteyenin tam adını ve parayı alacak IBAN'ı görür
        mvc.perform(get("/api/payment-requests").param("role", "IN").with(as(bob)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].direction").value("INCOMING"))
                .andExpect(jsonPath("$.content[0].counterpartyName").value("Ada Test"))
                .andExpect(jsonPath("$.content[0].requesterIban").value(adaAcc.getIban()));
        mvc.perform(get("/api/payment-requests/pending-count").with(as(bob))).andExpect(jsonPath("$.count").value(1));
        assertNotification(bob, "PAYMENT_REQUEST_RECEIVED");

        pay(bob, id, bobAcc)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.transactionId").isNumber());

        assertThat(data.balance(adaAcc)).isEqualByComparingTo("220.50");
        assertThat(data.balance(bobAcc)).isEqualByComparingTo("379.50");
        // İsteyen "isteğin ödendi" bildirimi alır; ayrıca "para geldi" bildirimi tekrarlanmaz
        assertThat(jdbc.queryForList("select type from notifications where user_id = ?", String.class, ada.getId()))
                .containsExactly("PAYMENT_REQUEST_PAID");
    }

    @Test
    void aRequestCanOnlyBePaidOnce() throws Exception {
        Integer id = idOf(create(ada, adaAcc, bobAcc.getIban(), "10"));
        pay(bob, id, bobAcc).andExpect(status().isOk());
        pay(bob, id, bobAcc)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUEST_NOT_PENDING"));
        assertThat(data.balance(bobAcc)).isEqualByComparingTo("490.00");
    }

    @Test
    void concurrentPayClicksMoveMoneyOnce() throws Exception {
        Long id = (long) idOf(create(ada, adaAcc, bobAcc.getIban(), "50"));
        AtomicInteger paid = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    service.pay(bob.getId(), id, bobAcc.getId());
                    paid.incrementAndGet();
                } catch (BusinessException e) {
                    assertThat(e.code()).isEqualTo(ErrorCode.REQUEST_NOT_PENDING);
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(paid).hasValue(1);
        assertThat(rejected).hasValue(7);
        assertThat(data.balance(bobAcc)).isEqualByComparingTo("450.00");
        assertThat(jdbc.queryForObject("select count(*) from transactions", Long.class)).isEqualTo(1);
    }

    @Test
    void onlyThePayerCanPayOrDeclineAndOnlyTheRequesterCanCancel() throws Exception {
        Integer id = idOf(create(ada, adaAcc, bobAcc.getIban(), "10"));

        pay(eve, id, data.account(eve, "100")).andExpect(status().isNotFound());
        mvc.perform(post("/api/payment-requests/{id}/decline", id).with(as(ada)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/payment-requests/{id}/cancel", id).with(as(bob)).with(csrf()))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/payment-requests/{id}/decline", id).with(as(bob)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
        assertNotification(ada, "PAYMENT_REQUEST_DECLINED");

        mvc.perform(post("/api/payment-requests/{id}/cancel", id).with(as(ada)).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void requesterCanCancelAndPayerIsTold() throws Exception {
        Integer id = idOf(create(ada, adaAcc, bobAcc.getIban(), "10"));
        mvc.perform(post("/api/payment-requests/{id}/cancel", id).with(as(ada)).with(csrf()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertNotification(bob, "PAYMENT_REQUEST_CANCELLED");
        pay(bob, id, bobAcc).andExpect(status().isConflict());
    }

    @Test
    void expiredRequestsCannotBePaid() throws Exception {
        Integer id = idOf(create(ada, adaAcc, bobAcc.getIban(), "10"));
        jdbc.update("update payment_requests set expires_at = now() - interval '1 minute' where id = ?", id);

        mvc.perform(get("/api/payment-requests").with(as(bob)))
                .andExpect(jsonPath("$.content[0].status").value("EXPIRED"));
        pay(bob, id, bobAcc).andExpect(status().isConflict());

        service.expireOverdue();
        assertThat(jdbc.queryForObject("select status from payment_requests where id = ?", String.class, id))
                .isEqualTo("EXPIRED");
    }

    @Test
    void cannotRequestFromYourselfOrIntoSomeoneElsesAccount() throws Exception {
        create(ada, adaAcc, data.account(ada, "0").getIban(), "10")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("REQUEST_SELF"));
        create(ada, bobAcc, bobAcc.getIban(), "10")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void payingStillEnforcesBalanceAndKeepsRequestOpen() throws Exception {
        Integer id = idOf(create(bob, bobAcc, adaAcc.getIban(), "150"));
        pay(ada, id, adaAcc)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
        mvc.perform(get("/api/payment-requests").with(as(ada)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].amount", eq("150")));
    }

    private ResultActions create(User requester, Account toAccount, String payerIban, String amount) throws Exception {
        return mvc.perform(post("/api/payment-requests").with(as(requester)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"toAccountId":%d,"payerIban":"%s","amount":%s,"description":"Konser bileti"}"""
                        .formatted(toAccount.getId(), payerIban, amount)));
    }

    private ResultActions pay(User payer, Integer id, Account from) throws Exception {
        return mvc.perform(post("/api/payment-requests/{id}/pay", id).with(as(payer)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":%d}".formatted(from.getId())));
    }

    private static Integer idOf(ResultActions actions) throws Exception {
        return JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private void assertNotification(User user, String type) {
        assertThat(jdbc.queryForList("select type from notifications where user_id = ?", String.class, user.getId()))
                .contains(type);
    }
}
