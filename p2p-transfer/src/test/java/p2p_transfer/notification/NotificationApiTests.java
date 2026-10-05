package p2p_transfer.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import p2p_transfer.account.Account;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.transfer.TransactionCategory;
import p2p_transfer.transfer.TransferDtos.TransferRequest;
import p2p_transfer.transfer.TransferService;
import p2p_transfer.user.User;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static p2p_transfer.support.Money.eq;
import static p2p_transfer.support.TestData.as;

class NotificationApiTests extends IntegrationTest {

    @Autowired
    TransferService transferService;

    @Autowired
    NotificationStreams streams;

    @Test
    void receiverIsNotifiedOfIncomingMoneyButSenderIsNot() throws Exception {
        User ada = data.user("Ada");
        User bob = data.user("Bob");
        Account adaAcc = data.account(ada, "500");
        Account bobAcc = data.account(bob, "0");
        send(ada, adaAcc, bobAcc, "75.50");

        mvc.perform(get("/api/notifications").with(as(bob)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("MONEY_RECEIVED"))
                .andExpect(jsonPath("$.content[0].amount", eq("75.50")))
                .andExpect(jsonPath("$.content[0].counterpartyName").value("Ada Test"))
                .andExpect(jsonPath("$.content[0].transactionId").isNumber())
                .andExpect(jsonPath("$.content[0].read").value(false));
        mvc.perform(get("/api/notifications").with(as(ada)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void transfersBetweenOwnAccountsAreSilent() {
        User ada = data.user("Ada");
        send(ada, data.account(ada, "100"), data.account(ada, "0"), "10");
        assertThat(jdbc.queryForObject("select count(*) from notifications", Long.class)).isZero();
    }

    @Test
    void failedTransferLeavesNoNotificationBehind() {
        User ada = data.user("Ada");
        Account from = data.account(ada, "5");
        Account to = data.account(data.user("Bob"), "0");
        try {
            send(ada, from, to, "10");
        } catch (RuntimeException expected) {
            // yetersiz bakiye: transfer ve bildirimi birlikte geri alınır
        }
        assertThat(jdbc.queryForObject("select count(*) from notifications", Long.class)).isZero();
    }

    @Test
    void unreadCountAndMarkingAsRead() throws Exception {
        User ada = data.user("Ada");
        User bob = data.user("Bob");
        Account adaAcc = data.account(ada, "500");
        Account bobAcc = data.account(bob, "0");
        send(ada, adaAcc, bobAcc, "1");
        send(ada, adaAcc, bobAcc, "2");

        mvc.perform(get("/api/notifications/unread-count").with(as(bob))).andExpect(jsonPath("$.count").value(2));

        Long id = jdbc.queryForObject("select min(id) from notifications", Long.class);
        mvc.perform(post("/api/notifications/{id}/read", id).with(as(ada)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/notifications/{id}/read", id).with(as(bob)).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notifications/unread-count").with(as(bob))).andExpect(jsonPath("$.count").value(1));

        mvc.perform(post("/api/notifications/read-all").with(as(bob)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/notifications").param("unreadOnly", "true").with(as(bob)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void streamEndpointOpensAnEventStream() throws Exception {
        User ada = data.user("Ada");
        mvc.perform(get("/api/notifications/stream").with(as(ada)).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
        assertThat(streams.connectionCount(ada.getId())).isEqualTo(1);
    }

    @Test
    void streamRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/notifications/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());
    }

    private void send(User user, Account from, Account to, String amount) {
        transferService.transfer(user.getId(),
                new TransferRequest(from.getId(), to.getIban(), new BigDecimal(amount), null, TransactionCategory.GENERAL), null);
    }
}
