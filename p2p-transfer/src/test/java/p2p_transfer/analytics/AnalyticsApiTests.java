package p2p_transfer.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import p2p_transfer.account.Account;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.transfer.TransactionCategory;
import p2p_transfer.transfer.TransferDtos.TransferRequest;
import p2p_transfer.transfer.TransferService;
import p2p_transfer.user.User;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static p2p_transfer.support.Money.eq;
import static p2p_transfer.support.TestData.as;

class AnalyticsApiTests extends IntegrationTest {

    @Autowired
    TransferService transferService;

    @Test
    void summarisesFlowsCategoriesAndTopRecipients() throws Exception {
        User ada = data.user("Ada");
        User bob = data.user("Bob");
        User cem = data.user("Cem");
        Account adaAcc = data.account(ada, "1000");
        Account bobAcc = data.account(bob, "1000");
        Account cemAcc = data.account(cem, "0");

        send(ada, adaAcc, bobAcc, "100", TransactionCategory.RENT);
        send(ada, adaAcc, bobAcc, "50", TransactionCategory.FOOD);
        send(ada, adaAcc, cemAcc, "30", TransactionCategory.FOOD);
        send(bob, bobAcc, adaAcc, "200", null);
        // kendi hesabına virman "en çok gönderilenler"de görünmemeli
        send(ada, adaAcc, data.account(ada, "0"), "500", TransactionCategory.SAVINGS);

        mvc.perform(get("/api/analytics/summary").param("accountId", adaAcc.getId().toString())
                        .param("months", "3").with(as(ada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthly.length()").value(3))
                .andExpect(jsonPath("$.totalIncoming", eq("200")))
                .andExpect(jsonPath("$.totalOutgoing", eq("680")))
                .andExpect(jsonPath("$.topRecipients.length()").value(2))
                .andExpect(jsonPath("$.net", eq("-480")))
                .andExpect(jsonPath("$.monthly[2].outgoing", eq("680")))
                .andExpect(jsonPath("$.byCategory[0].category").value("SAVINGS"))
                .andExpect(jsonPath("$.byCategory[1].category").value("RENT"))
                .andExpect(jsonPath("$.byCategory[2].category").value("FOOD"))
                .andExpect(jsonPath("$.byCategory[2].total", eq("80")))
                .andExpect(jsonPath("$.byCategory[2].count").value(2))
                .andExpect(jsonPath("$.topRecipients[0].name").value("Bob Test"))
                .andExpect(jsonPath("$.topRecipients[0].total", eq("150")));
    }

    @Test
    void cannotReadAnotherUsersAnalytics() throws Exception {
        Account bobAcc = data.account(data.user("Bob"), "0");
        mvc.perform(get("/api/analytics/summary").param("accountId", bobAcc.getId().toString()).with(as(data.user("Eve"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void monthsParameterIsBounded() throws Exception {
        User ada = data.user("Ada");
        Account acc = data.account(ada, "0");
        mvc.perform(get("/api/analytics/summary").param("accountId", acc.getId().toString())
                        .param("months", "60").with(as(ada)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private void send(User user, Account from, Account to, String amount, TransactionCategory category) {
        transferService.transfer(user.getId(), new TransferRequest(from.getId(), to.getIban(), new BigDecimal(amount), null, category), null);
    }
}
